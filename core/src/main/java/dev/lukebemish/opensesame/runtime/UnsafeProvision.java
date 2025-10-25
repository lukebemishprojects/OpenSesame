package dev.lukebemish.opensesame.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

final class UnsafeProvision {
    private UnsafeProvision() {}
    
    private static final Object theUnsafe;
    
    static {
        Object found;
        try {
            found = locateTheUnsafe();
        } catch (Exception e) {
            try {
                var unsafeModule = ModuleLayer.boot().findModule("jdk.unsupported").orElseThrow();
                var thisModule = LookupProviderUnsafe.class.getModule();
                thisModule.addReads(unsafeModule);
            } catch (Exception e2) {
                var exception = new RuntimeException("Could not require jdk.unsupported module to use unsafe", e2);
                exception.addSuppressed(e);
                throw exception;
            }

            try {
                found = locateTheUnsafe();
            } catch (Exception e2) {
                var exception = new RuntimeException("Issue locating instance of sun.misc.Unsafe", e2);
                exception.addSuppressed(e);
                throw exception;
            }
        }
        theUnsafe = found;
    }
    
    static Object theUnsafe() {
        return theUnsafe;
    }
    
    private static Object locateTheUnsafe() throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe", true, UnsafeProvision.class.getClassLoader());

        Field[] fields = unsafe.getDeclaredFields();
        Field f = null;
        for (var field : fields) {
            if (field.getType() == unsafe && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                f = field;
                break;
            }
        }
        if (f == null) {
            throw new IllegalStateException("Could not find static final Unsafe-typed field in sun.misc.Unsafe");
        }

        f.setAccessible(true);
        return f.get(null);
    }
}
