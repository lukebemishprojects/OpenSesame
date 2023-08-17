package dev.lukebemish.opensesame.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A series of metafactories that generate call sites for otherwise inaccessible members of other classes.
 */
public final class OpeningMetafactory {
    private OpeningMetafactory() {}

    private static final Map<ClassLoaderKey, RuntimeRemapper> REMAPPER_LOOKUP = new ConcurrentHashMap<>();
    private static final ReferenceQueue<ClassLoader> REMAPPER_LOOKUP_QUEUE = new ReferenceQueue<>();

    private static RuntimeRemapper getRemapper(ClassLoader classLoader) {
        ClassLoaderKey ref;
        while ((ref = (ClassLoaderKey) REMAPPER_LOOKUP_QUEUE.poll()) != null) {
            REMAPPER_LOOKUP.remove(ref);
        }
        return REMAPPER_LOOKUP.computeIfAbsent(new ClassLoaderKey(classLoader, REMAPPER_LOOKUP_QUEUE), k ->
                ServiceLoader.load(RuntimeRemapper.class, classLoader).findFirst().orElse(null));
    }

    private static class ClassLoaderKey extends WeakReference<ClassLoader> {
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

    public static CallSite invokeStatic(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            RuntimeRemapper remapper = getRemapper(caller.lookupClass().getClassLoader());
            if (remapper != null) {
                String remapMethodName = remapper.remapMethodName(holdingClass, targetMethodName, factoryType.parameterArray());
                if (remapMethodName != null) {
                    targetMethodName = remapMethodName;
                }
            }
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findStatic(holdingClass, targetMethodName, factoryType);
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite invokeInstance(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            RuntimeRemapper remapper = getRemapper(caller.lookupClass().getClassLoader());
            if (remapper != null) {
                String remapMethodName = remapper.remapMethodName(holdingClass, targetMethodName, factoryType.parameterArray());
                if (remapMethodName != null) {
                    targetMethodName = remapMethodName;
                }
            }
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findVirtual(holdingClass, targetMethodName, factoryType.dropParameterTypes(0, 1));
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite invokePrivateInstance(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            RuntimeRemapper remapper = getRemapper(caller.lookupClass().getClassLoader());
            if (remapper != null) {
                String remapMethodName = remapper.remapMethodName(holdingClass, targetMethodName, factoryType.parameterArray());
                if (remapMethodName != null) {
                    targetMethodName = remapMethodName;
                }
            }
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findSpecial(holdingClass, targetMethodName, factoryType.dropParameterTypes(0, 1), holdingClass);
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite invokeStaticFieldGet(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            RuntimeRemapper remapper = getRemapper(caller.lookupClass().getClassLoader());
            if (remapper != null) {
                String remapMethodName = remapper.remapFieldName(holdingClass, targetMethodName);
                if (remapMethodName != null) {
                    targetMethodName = remapMethodName;
                }
            }
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findStaticGetter(holdingClass, targetMethodName, factoryType.returnType());
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite invokeInstanceFieldGet(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            RuntimeRemapper remapper = getRemapper(caller.lookupClass().getClassLoader());
            if (remapper != null) {
                String remapMethodName = remapper.remapFieldName(holdingClass, targetMethodName);
                if (remapMethodName != null) {
                    targetMethodName = remapMethodName;
                }
            }
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findGetter(holdingClass, targetMethodName, factoryType.returnType());
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite invokeStaticFieldSet(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            RuntimeRemapper remapper = getRemapper(caller.lookupClass().getClassLoader());
            if (remapper != null) {
                String remapMethodName = remapper.remapFieldName(holdingClass, targetMethodName);
                if (remapMethodName != null) {
                    targetMethodName = remapMethodName;
                }
            }
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findStaticSetter(holdingClass, targetMethodName, factoryType.parameterType(0));
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite invokeInstanceFieldSet(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            RuntimeRemapper remapper = getRemapper(caller.lookupClass().getClassLoader());
            if (remapper != null) {
                String remapMethodName = remapper.remapFieldName(holdingClass, targetMethodName);
                if (remapMethodName != null) {
                    targetMethodName = remapMethodName;
                }
            }
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findSetter(holdingClass, targetMethodName, factoryType.parameterType(1));
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite invokeCtor(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findConstructor(holdingClass, factoryType.changeReturnType(Void.TYPE));
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
