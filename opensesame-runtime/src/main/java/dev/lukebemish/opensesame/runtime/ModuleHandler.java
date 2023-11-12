package dev.lukebemish.opensesame.runtime;

import sun.misc.Unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

class ModuleHandler {
    private final Unsafe theUnsafe;
    private final Method[] methods = new Method[1];

    private final Consumer<AccessibleObject> opener;

    ModuleHandler() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            theUnsafe = (Unsafe) f.get(null);

            Field field = AccessibleObject.class.getDeclaredField("override");
            final long offset = theUnsafe.objectFieldOffset(field);
            opener = ao -> theUnsafe.putBoolean(ao, offset, true);

            Method implAddExportsOrOpens = Module.class.getDeclaredMethod(
                    "implAddExportsOrOpens",
                    String.class,
                    Module.class,
                    boolean.class,
                    boolean.class
            );
            opener.accept(implAddExportsOrOpens);
            methods[0] = implAddExportsOrOpens;
        } catch (Throwable e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e);
        }
    }

    void openModule(Module to, String moduleName, String[] classNames) {
        try {
            Module targetModule = findModule(moduleName);
            Set<String> packages = new HashSet<>();
            for (String className : classNames) {
                packages.add(className.substring(0, className.lastIndexOf('/')).replace('/', '.'));
            }

            for (String p : packages) {
                if (targetModule.getPackages().contains(p)) {
                    methods[0].invoke(targetModule, p, to, true, true);
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Module findModule(String moduleName) {
        return ModuleLayer.boot().findModule(moduleName).orElseThrow();
    }
}
