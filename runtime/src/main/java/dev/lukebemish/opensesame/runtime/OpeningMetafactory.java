package dev.lukebemish.opensesame.runtime;

import java.lang.invoke.*;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * A series of metafactories that generate call sites for otherwise inaccessible members of other classes.
 */
public final class OpeningMetafactory {
    private OpeningMetafactory() {}

    private static final class ClassLoaderKey extends WeakReference<ClassLoader> {
        final int hashCode;

        public ClassLoaderKey(ClassLoader referent, ReferenceQueue<? super ClassLoader> q) {
            super(referent, q);
            this.hashCode = System.identityHashCode(referent);
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof ClassLoaderKey key) && hashCode == key.hashCode && get() == key.get();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private static final Map<ClassLoaderKey, List<RuntimeRemapper>> REMAPPER_LOOKUP = new HashMap<>();
    private static final ReferenceQueue<ClassLoader> REMAPPER_LOOKUP_QUEUE = new ReferenceQueue<>();
    private static final LookupProvider LOOKUP_PROVIDER;
    private static final Exception LOOKUP_PROVIDER_EXCEPTION;

    public static final int STATIC_TYPE = 0;
    public static final int INSTANCE_TYPE = 1;
    public static final int PRIVATE_INSTANCE_TYPE = 2;
    public static final int STATIC_GET_TYPE = 3;
    public static final int INSTANCE_GET_TYPE = 4;
    public static final int STATIC_SET_TYPE = 5;
    public static final int INSTANCE_SET_TYPE = 6;
    public static final int CTOR_TYPE = 7;

    static {
        Exception LOOKUP_PROVIDER_EXCEPTION1;
        LookupProvider LOOKUP_PROVIDER1;
        try {
            LOOKUP_PROVIDER1 = new LookupProviderUnsafe();
            LOOKUP_PROVIDER_EXCEPTION1 = null;
        } catch (Exception e) {
            LOOKUP_PROVIDER_EXCEPTION1 = e;
            LOOKUP_PROVIDER1 = new LookupProviderFallback();
        }
        LOOKUP_PROVIDER_EXCEPTION = LOOKUP_PROVIDER_EXCEPTION1;
        LOOKUP_PROVIDER = LOOKUP_PROVIDER1;
    }

    private synchronized static List<RuntimeRemapper> getRemapper(ClassLoader classLoader) {
        ClassLoaderKey ref;
        while ((ref = (ClassLoaderKey) REMAPPER_LOOKUP_QUEUE.poll()) != null) {
            REMAPPER_LOOKUP.remove(ref);
        }
        return REMAPPER_LOOKUP.computeIfAbsent(new ClassLoaderKey(classLoader, REMAPPER_LOOKUP_QUEUE), k ->
                ServiceLoader.load(RuntimeRemapper.class, classLoader).stream().map(ServiceLoader.Provider::get).toList()
        );
    }

    @SuppressWarnings("unused")
    public static CallSite invoke(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, MethodHandle classProvider, MethodHandle accessTypeProvider, int type) {
        Class<?> holdingClass;
        MethodType accessType;
        try {
            holdingClass = (Class<?>) classProvider.invoke(caller.lookupClass().getClassLoader());
            accessType = (MethodType) accessTypeProvider.invoke(caller.lookupClass().getClassLoader());
        } catch (Throwable throwable) {
            throw new OpeningException(throwable);
        }
        return invoke0(caller, targetMethodName, factoryType, accessType, holdingClass, type);
    }

    @SuppressWarnings("unused")
    public static CallSite invoke(MethodHandles.Lookup caller, String name, MethodType factoryType, Class<?> holdingClass, int type) {
        return invoke0(caller, name, factoryType, factoryType, holdingClass, type);
    }

    private static CallSite invoke0(MethodHandles.Lookup caller, String name, MethodType factoryType, MethodType accessType, Class<?> holdingClass, int type) {
        if (type < STATIC_GET_TYPE) {
            name = remapMethod(name, accessType, holdingClass, caller.lookupClass().getClassLoader());
        } else if (type < CTOR_TYPE) {
            name = remapField(name, holdingClass, caller.lookupClass().getClassLoader());
        }
        var handle = makeHandle(caller, name, factoryType, accessType, holdingClass, type);
        return new ConstantCallSite(handle);
    }

    private static MethodHandle makeHandle(MethodHandles.Lookup caller, String name, MethodType factoryType, MethodType accessType, Class<?> holdingClass, int type) {
        try {
            var lookup = LOOKUP_PROVIDER.openingLookup(caller, holdingClass);
            var handle = switch (type) {
                case STATIC_TYPE -> lookup.findStatic(holdingClass, name, accessType);
                case INSTANCE_TYPE -> lookup.findVirtual(holdingClass, name, accessType.dropParameterTypes(0, 1));
                case PRIVATE_INSTANCE_TYPE -> lookup.findSpecial(holdingClass, name, accessType.dropParameterTypes(0, 1), holdingClass);
                case STATIC_GET_TYPE -> lookup.findStaticGetter(holdingClass, name, accessType.returnType());
                case INSTANCE_GET_TYPE -> lookup.findGetter(holdingClass, name, accessType.returnType());
                case STATIC_SET_TYPE -> lookup.findStaticSetter(holdingClass, name, accessType.parameterType(0));
                case INSTANCE_SET_TYPE -> lookup.findSetter(holdingClass, name, accessType.parameterType(1));
                case CTOR_TYPE -> lookup.findConstructor(holdingClass, accessType.changeReturnType(Void.TYPE));
                default -> throw new OpeningException("Unexpected opening type: " + type);
            };
            return handle.asType(factoryType);
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            var exception = new OpeningException("Issue creating method handle for `"+name+"`", e);

            if (LOOKUP_PROVIDER_EXCEPTION != null) {
                exception.addSuppressed(LOOKUP_PROVIDER_EXCEPTION);
            }

            throw exception;
        }
    }

    private static String remapMethod(String targetMethodName, MethodType methodType, Class<?> holdingClass, ClassLoader classLoader) {
        List<RuntimeRemapper> remappers = getRemapper(classLoader);
        for (var remapper : remappers) {
            String remapMethodName = remapper.remapMethodName(holdingClass, targetMethodName, methodType.parameterArray());
            if (remapMethodName != null) {
                return remapMethodName;
            }
        }
        return targetMethodName;
    }

    private static String remapField(String targetFieldName, Class<?> holdingClass, ClassLoader classLoader) {
        List<RuntimeRemapper> remappers = getRemapper(classLoader);
        for (var remapper : remappers) {
            String remapFieldName = remapper.remapFieldName(holdingClass, targetFieldName);
            if (remapFieldName != null) {
                return remapFieldName;
            }
        }
        return targetFieldName;
    }

    @SuppressWarnings("unused")
    public static String remapClass(String className, ClassLoader classLoader) {
        List<RuntimeRemapper> remappers = getRemapper(classLoader);
        for (var remapper : remappers) {
            String remapClassName = remapper.remapClassName(className);
            if (remapClassName != null) {
                return remapClassName;
            }
        }
        return className;
    }
}
