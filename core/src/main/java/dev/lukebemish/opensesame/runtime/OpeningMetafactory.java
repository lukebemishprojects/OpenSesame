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

    /**
     * Invoke a static method
     */
    public static final int STATIC_TYPE = 0;
    /**
     * Invoke an instance method virtually
     */
    public static final int VIRTUAL_TYPE = 1;
    /**
     * Invoke an instance method non-virtually
     */
    public static final int SPECIAL_TYPE = 2;
    /**
     * Get a static field
     */
    public static final int STATIC_GET_TYPE = 3;
    /**
     * Get an instance field
     */
    public static final int INSTANCE_GET_TYPE = 4;
    /**
     * Set a static field
     */
    public static final int STATIC_SET_TYPE = 5;
    /**
     * Set an instance field
     */
    public static final int INSTANCE_SET_TYPE = 6;
    /**
     * Invoke a constructor
     */
    public static final int CONSTRUCT_TYPE = 7;
    /**
     * Create an array
     */
    public static final int ARRAY_TYPE = 8;

    /**
     * Creates a call site of a member of a class in a "safe" fashion (obeying module boundaries).
     * @param caller the lookup of the caller
     * @param targetMethodName the name of the member to open
     * @param factoryType the type of the call site to create
     * @param classProvider a method handle that takes a classloader and returns the class to open
     * @param accessTypeProvider a method handle that takes a classloader and returns a type matching the member to
     *                           open, which should be convertible to the {@code factoryType}
     * @param type the type of member to look up, as determined by the constants in this class
     * @return the call site
     * @throws OpeningException if the member could not be found or could not be opened
     */
    @SuppressWarnings("unused")
    public static CallSite invoke(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, MethodHandle classProvider, MethodHandle accessTypeProvider, int type) {
        return invoke0(caller, targetMethodName, factoryType, classProvider, accessTypeProvider, type, false);
    }

    /**
     * Creates a call site of a member of a class in an unsafe fashion, breaking through module boundaries
     * @param caller the lookup of the caller
     * @param targetMethodName the name of the member to open
     * @param factoryType the type of the call site to create
     * @param classProvider a method handle that takes a classloader and returns the class to open
     * @param accessTypeProvider a method handle that takes a classloader and returns a type matching the member to
     *                           open, which should be convertible to the {@code factoryType}
     * @param type the type of member to look up, as determined by the constants in this class
     * @return the call site
     * @throws OpeningException if the member could not be found or could not be opened
     */
    @SuppressWarnings("unused")
    public static CallSite invokeUnsafe(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, MethodHandle classProvider, MethodHandle accessTypeProvider, int type) {
        try {
            return invoke0(caller, targetMethodName, factoryType, classProvider, accessTypeProvider, type, true);
        } catch (RuntimeException e) {
            var exception = new OpeningException(e);
            if (LOOKUP_PROVIDER_EXCEPTION != null) {
                exception.addSuppressed(LOOKUP_PROVIDER_EXCEPTION);
            }
            throw exception;
        }
    }

    /**
     * Creates a call site of a class known ahead of time in a "safe" fashion (obeying module boundaries).
     * @param caller the lookup of the caller
     * @param name the name of the member to open
     * @param factoryType the type of the call site
     * @param holdingClass the class that holds the member
     * @param type the type of member to look up, as determined by the constants in this class
     * @return the call site
     * @throws OpeningException if the member could not be found or could not be opened
     */
    @SuppressWarnings("unused")
    public static CallSite invokeKnown(MethodHandles.Lookup caller, String name, MethodType factoryType, Class<?> holdingClass, int type) {
        return invoke1(caller, name, factoryType, factoryType, holdingClass, type, false);
    }

    /**
     * Creates a call site of a class known ahead of time in an unsafe fashion, breaking through module boundaries
     * @param caller the lookup of the caller
     * @param name the name of the member to open
     * @param factoryType the type of the call site
     * @param holdingClass the class that holds the member
     * @param type the type of member to look up, as determined by the constants in this class
     * @return the call site
     * @throws OpeningException if the member could not be found or could not be opened
     */
    @SuppressWarnings("unused")
    public static CallSite invokeKnownUnsafe(MethodHandles.Lookup caller, String name, MethodType factoryType, Class<?> holdingClass, int type) {
        return invoke1(caller, name, factoryType, factoryType, holdingClass, type, true);
    }
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
    private static final LookupProvider LOOKUP_PROVIDER_UNSAFE;
    private static final LookupProvider LOOKUP_PROVIDER_SAFE = new LookupProviderFallback();
    private static final Exception LOOKUP_PROVIDER_EXCEPTION;

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
        LOOKUP_PROVIDER_UNSAFE = LOOKUP_PROVIDER1;
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

    private static CallSite invoke0(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, MethodHandle classProvider, MethodHandle accessTypeProvider, int type, boolean unsafe) {
        Class<?> holdingClass;
        MethodType accessType;
        try {
            holdingClass = (Class<?>) classProvider.invoke(caller.lookupClass().getClassLoader());
            accessType = (MethodType) accessTypeProvider.invoke(caller.lookupClass().getClassLoader());
        } catch (Throwable throwable) {
            throw new OpeningException(throwable);
        }
        return invoke1(caller, targetMethodName, factoryType, accessType, holdingClass, type, unsafe);
    }

    private static CallSite invoke1(MethodHandles.Lookup caller, String name, MethodType factoryType, MethodType accessType, Class<?> holdingClass, int type, boolean unsafe) {
        MethodHandles.Lookup lookup;
        try {
            if (unsafe) {
                lookup = LOOKUP_PROVIDER_UNSAFE.openingLookup(caller, holdingClass);
            } else {
                lookup = LOOKUP_PROVIDER_SAFE.openingLookup(caller, holdingClass);
            }
        } catch (IllegalAccessException e) {
            throw new OpeningException("Issue creating lookup", e);
        }
        if (type < STATIC_GET_TYPE) {
            name = remapMethod(name, accessType, holdingClass, caller.lookupClass().getClassLoader());
        } else if (type < CONSTRUCT_TYPE) {
            name = remapField(name, holdingClass, caller.lookupClass().getClassLoader());
        }
        var handle = makeHandle(lookup, name, factoryType, accessType, holdingClass, type);
        return new ConstantCallSite(handle);
    }

    private static MethodHandle makeHandle(MethodHandles.Lookup lookup, String name, MethodType factoryType, MethodType accessType, Class<?> holdingClass, int type) {
        try {
            var handle = switch (type) {
                case STATIC_TYPE -> lookup.findStatic(holdingClass, name, accessType);
                case VIRTUAL_TYPE -> lookup.findVirtual(holdingClass, name, accessType.dropParameterTypes(0, 1));
                case SPECIAL_TYPE -> lookup.findSpecial(holdingClass, name, accessType.dropParameterTypes(0, 1), holdingClass);
                case STATIC_GET_TYPE -> lookup.findStaticGetter(holdingClass, name, accessType.returnType());
                case INSTANCE_GET_TYPE -> lookup.findGetter(holdingClass, name, accessType.returnType());
                case STATIC_SET_TYPE -> lookup.findStaticSetter(holdingClass, name, accessType.parameterType(0));
                case INSTANCE_SET_TYPE -> lookup.findSetter(holdingClass, name, accessType.parameterType(1));
                case CONSTRUCT_TYPE -> lookup.findConstructor(holdingClass, accessType.changeReturnType(Void.TYPE));
                case ARRAY_TYPE -> MethodHandles.arrayConstructor(holdingClass.arrayType());
                default -> throw new OpeningException("Unexpected opening type: " + type);
            };
            return handle.asType(factoryType);
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            throw new OpeningException("Issue creating method handle for `"+name+"`", e);
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

    /**
     * Remap a class name.
     * @param className the class name to remap
     * @param classLoader the classloader the caller of {@link OpeningMetafactory} is using
     * @return the remapped class name, or the original if no remapping was found
     */
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
