package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@ApiStatus.Internal
class LookupProviderUnsafe implements LookupProvider {

    private static final String JDK_UNSUPPORTED = "jdk.unsupported";

    private final MethodHandles.Lookup lookup;

    LookupProviderUnsafe() {
        MethodHandles.Lookup found;
        try {
            found = getImplLookup();
        } catch (Exception e) {
            try {
                var unsafeModule = ModuleLayer.boot().findModule(JDK_UNSUPPORTED).orElseThrow();
                var thisModule = LookupProviderUnsafe.class.getModule();
                thisModule.addReads(unsafeModule);
            } catch (Exception e2) {
                var exception = new RuntimeException("Could not require jdk.unsupported module to use unsafe", e2);
                exception.addSuppressed(e);
                throw exception;
            }

            try {
                found = getImplLookup();
            } catch (Exception e2) {
                var exception = new RuntimeException("Issue setting up unsafe lookup provider", e2);
                exception.addSuppressed(e);
                throw exception;
            }
        }
        this.lookup = found;
    }

    private MethodHandles.Lookup getImplLookup() throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe", true, LookupProviderUnsafe.class.getClassLoader());
        
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
        Object theUnsafe = f.get(null);

        var implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        Method staticFieldBase = unsafe.getDeclaredMethod("staticFieldBase", Field.class);
        Method staticFieldOffset = unsafe.getDeclaredMethod("staticFieldOffset", Field.class);
        Object base = staticFieldBase.invoke(theUnsafe, implLookupField);
        long offset = (long) staticFieldOffset.invoke(theUnsafe, implLookupField);
        Method getObject = unsafe.getDeclaredMethod("getObject", Object.class, long.class);
        return (MethodHandles.Lookup)
                getObject.invoke(theUnsafe, base, offset);
    }

    @Override
    public MethodHandles.Lookup openingLookup(MethodHandles.Lookup original, Class<?> target) {
        return lookup;
    }
}
