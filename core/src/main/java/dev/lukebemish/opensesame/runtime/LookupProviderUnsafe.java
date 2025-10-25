package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@ApiStatus.Internal
class LookupProviderUnsafe implements LookupProvider {

    private final MethodHandles.Lookup lookup;

    LookupProviderUnsafe() {
        try {
            this.lookup = getImplLookup();
        } catch (Exception e) {
            throw new RuntimeException("Issue obtaining IMPL_LOOKUP via unsafe", e);
        }
    }

    private MethodHandles.Lookup getImplLookup() throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe", true, LookupProviderUnsafe.class.getClassLoader());
        Object theUnsafe = UnsafeProvision.theUnsafe();

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
