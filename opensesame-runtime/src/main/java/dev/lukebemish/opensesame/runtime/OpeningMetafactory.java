package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * A series of metafactories that generate call sites for otherwise inaccessible members of other classes.
 */
public final class OpeningMetafactory {
    private OpeningMetafactory() {}

    private static final Map<ClassLoaderKey, RuntimeRemapper> REMAPPER_LOOKUP = new HashMap<>();
    private static final ReferenceQueue<ClassLoader> REMAPPER_LOOKUP_QUEUE = new ReferenceQueue<>();
    private static final ModuleHandler MODULE_HANDLER;
    private static final Throwable MODULE_HANDLER_EXCEPTION;

    static {
        Throwable MODULE_HANDLER_EXCEPTION1;
        ModuleHandler MODULE_HANDLER1;
        try {
            MODULE_HANDLER1 = new ModuleHandler();
            MODULE_HANDLER_EXCEPTION1 = null;
        } catch (Throwable e) {
            MODULE_HANDLER_EXCEPTION1 = e;
            MODULE_HANDLER1 = null;
        }
        MODULE_HANDLER_EXCEPTION = MODULE_HANDLER_EXCEPTION1;
        MODULE_HANDLER = MODULE_HANDLER1;
    }

    private synchronized static RuntimeRemapper getRemapper(ClassLoader classLoader) {
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

    private static Class<?> getOpenedClass(String holdingClasses, @Nullable String module, MethodHandles.Lookup caller) {
        Class<?> lookupClass = caller.lookupClass();
        ClassLoader classLoader = lookupClass.getClassLoader();
        String[] parts = holdingClasses.split(";");
        Class<?> holdingClass = null;
        List<Throwable> exceptions = new ArrayList<>();

        Module from = lookupClass.getModule();
        if (module != null && !module.isEmpty()) {
            if (MODULE_HANDLER == null) {
                throw new OpeningException(MODULE_HANDLER_EXCEPTION);
            }
            MODULE_HANDLER.openModule(from, module, parts);
        }

        for (String part : parts) {
            try {
                holdingClass = Class.forName(part, false, classLoader);
            } catch (ClassNotFoundException e) {
                exceptions.add(e);
            }
        }
        if (holdingClass == null) {
            var e = new OpeningException("No classes found out of "+ Arrays.toString(parts));
            for (var throwable : exceptions) {
                e.addSuppressed(throwable);
            }
            throw e;
        }
        return holdingClass;
    }
    
    public static CallSite invokeStatic(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, String holdingClasses, String module) {
        // TODO: handle coercion of types
        Class<?> holdingClass = getOpenedClass(holdingClasses, module, caller);
        return invokeStatic(caller, targetMethodName, factoryType, holdingClass);
    }

    public static CallSite invokeInstance(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, String holdingClasses, String module) {
        Class<?> holdingClass = getOpenedClass(holdingClasses, module, caller);
        return invokeInstance(caller, targetMethodName, factoryType, holdingClass);
    }

    public static CallSite invokePrivateInstance(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, String holdingClasses, String module) {
        Class<?> holdingClass = getOpenedClass(holdingClasses, module, caller);
        return invokePrivateInstance(caller, targetMethodName, factoryType, holdingClass);
    }

    public static CallSite invokeStaticFieldGet(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, String holdingClasses, String module) {
        Class<?> holdingClass = getOpenedClass(holdingClasses, module, caller);
        return invokeStaticFieldGet(caller, targetMethodName, factoryType, holdingClass);
    }

    public static CallSite invokeInstanceFieldGet(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, String holdingClasses, String module) {
        Class<?> holdingClass = getOpenedClass(holdingClasses, module, caller);
        return invokeInstanceFieldGet(caller, targetMethodName, factoryType, holdingClass);
    }

    public static CallSite invokeStaticFieldSet(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, String holdingClasses, String module) {
        Class<?> holdingClass = getOpenedClass(holdingClasses, module, caller);
        return invokeStaticFieldSet(caller, targetMethodName, factoryType, holdingClass);
    }

    public static CallSite invokeInstanceFieldSet(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, String holdingClasses, String module) {
        Class<?> holdingClass = getOpenedClass(holdingClasses, module, caller);
        return invokeInstanceFieldSet(caller, targetMethodName, factoryType, holdingClass);
    }

    public static CallSite invokeCtor(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, String holdingClasses, String module) {
        Class<?> holdingClass = getOpenedClass(holdingClasses, module, caller);
        return invokeCtor(caller, targetMethodName, factoryType, holdingClass);
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
            throw new OpeningException(e);
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
            throw new OpeningException(e);
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
            throw new OpeningException(e);
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
            throw new OpeningException(e);
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
            throw new OpeningException(e);
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
            throw new OpeningException(e);
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
            throw new OpeningException(e);
        }
    }

    public static CallSite invokeCtor(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findConstructor(holdingClass, factoryType.changeReturnType(Void.TYPE));
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new OpeningException(e);
        }
    }
}
