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
            Class<?> unsafe = Class.forName("sun.misc.Unsafe");
            Field f = unsafe.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object theUnsafe = f.get(null);

            var implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Method staticFieldBase = unsafe.getDeclaredMethod("staticFieldBase", Field.class);
            Method staticFieldOffset = unsafe.getDeclaredMethod("staticFieldOffset", Field.class);
            Method getObject = unsafe.getDeclaredMethod("getObject", Object.class, long.class);
            this.lookup = (MethodHandles.Lookup)
                    getObject.invoke(theUnsafe, staticFieldBase.invoke(theUnsafe, implLookupField), staticFieldOffset.invoke(theUnsafe, implLookupField));
        } catch (Exception e) {
            throw new RuntimeException("Issue setting up unsafe lookup provider", e);
        }
    }

    @Override
    public MethodHandles.Lookup openingLookup(MethodHandles.Lookup original, Class<?> target) {
        return lookup;
    }
}
