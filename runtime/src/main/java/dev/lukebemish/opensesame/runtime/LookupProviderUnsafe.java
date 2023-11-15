package dev.lukebemish.opensesame.runtime;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

class LookupProviderUnsafe implements LookupProvider {

    private final MethodHandles.Lookup lookup;

    LookupProviderUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe theUnsafe = (Unsafe) f.get(null);

            var implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            this.lookup = (MethodHandles.Lookup)
                    theUnsafe.getObject(theUnsafe.staticFieldBase(implLookupField), theUnsafe.staticFieldOffset(implLookupField));
        } catch (Exception e) {
            throw new RuntimeException("Issue setting up unsafe lookup provider", e);
        }
    }

    @Override
    public MethodHandles.Lookup openingLookup(Class<?> target) {
        return lookup;
    }
}
