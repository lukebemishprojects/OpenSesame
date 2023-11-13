package dev.lukebemish.opensesame.runtime;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

class ModuleHandlerUnsafe implements ModuleHandler {

    private final Method[] methods = new Method[1];

    ModuleHandlerUnsafe() {
        try {
            if (!PrecariouslyLifted.SETUP) {
                throw new RuntimeException("Unsafe module handler not available", PrecariouslyLifted.SETUP_EXCEPTION);
            }
            Method implAddExportsOrOpens = Module.class.getDeclaredMethod(
                    "implAddExportsOrOpens",
                    String.class,
                    Module.class,
                    boolean.class,
                    boolean.class
            );
            PrecariouslyLifted.OPENER.accept(implAddExportsOrOpens);
            methods[0] = implAddExportsOrOpens;
        } catch (Exception e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e);
        }
    }

    public boolean openModule(Module to, Module target, String className) {
        if (to == target) return true;
        try {
            String packageName = className.substring(0, className.lastIndexOf('.'));

            if (target.getPackages().contains(packageName)) {
                if (!target.isOpen(packageName, to)) {
                    methods[0].invoke(target, packageName, to, true, true);
                }
                return true;
            }
            return false;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class PrecariouslyLifted {
        private PrecariouslyLifted() {}

        private static final Consumer<AccessibleObject> OPENER;
        private static final boolean SETUP;
        private static final Exception SETUP_EXCEPTION;

        static {
            Consumer<AccessibleObject> OPENER1;
            boolean SETUP1;
            Exception SETUP_EXCEPTION1;
            try {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                Unsafe theUnsafe = (Unsafe) f.get(null);

                final Field classModule = Class.class.getDeclaredField("module");
                final long offset = theUnsafe.objectFieldOffset(classModule);
                theUnsafe.putObject(PrecariouslyLifted.class, offset, Object.class.getModule());

                Method setAccessible = AccessibleObject.class.getDeclaredMethod("setAccessible0", boolean.class);
                setAccessible.setAccessible(true);
                OPENER1 = ao -> {
                    try {
                        setAccessible.invoke(ao, true);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                };

                SETUP1 = true;
                SETUP_EXCEPTION1 = null;
            } catch (Exception e) {
                OPENER1 = null;
                SETUP1 = false;
                SETUP_EXCEPTION1 = e;
            }
            OPENER = OPENER1;
            SETUP = SETUP1;
            SETUP_EXCEPTION = SETUP_EXCEPTION1;
        }
    }
}
