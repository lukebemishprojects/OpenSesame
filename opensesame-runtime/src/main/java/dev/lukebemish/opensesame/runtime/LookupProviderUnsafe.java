package dev.lukebemish.opensesame.runtime;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

class LookupProviderUnsafe implements LookupProvider {

    private final MethodHandles.Lookup lookup;

    LookupProviderUnsafe() {
        if (!PrecariouslyLifted.SETUP) {
            throw new RuntimeException("Unsafe module handler not available", PrecariouslyLifted.SETUP_EXCEPTION);
        }
        this.lookup = PrecariouslyLifted.TRUSTED_LOOKUP;
    }

    @Override
    public MethodHandles.Lookup openingLookup(Class<?> target) {
        return lookup;
    }

    private static final class PrecariouslyLifted {
        private PrecariouslyLifted() {}

        private static final MethodHandles.Lookup TRUSTED_LOOKUP;
        private static final boolean SETUP;
        private static final Exception SETUP_EXCEPTION;

        static {
            boolean SETUP1;
            Exception SETUP_EXCEPTION1;
            MethodHandles.Lookup TRUSTED_LOOKUP1;
            try {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                Unsafe theUnsafe = (Unsafe) f.get(null);

                var implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
                TRUSTED_LOOKUP1 = (MethodHandles.Lookup)
                        theUnsafe.getObject(theUnsafe.staticFieldBase(implLookupField), theUnsafe.staticFieldOffset(implLookupField));

                SETUP1 = true;
                SETUP_EXCEPTION1 = null;
            } catch (Exception e) {
                TRUSTED_LOOKUP1 = null;
                SETUP1 = false;
                SETUP_EXCEPTION1 = e;
            }
            SETUP = SETUP1;
            SETUP_EXCEPTION = SETUP_EXCEPTION1;
            TRUSTED_LOOKUP = TRUSTED_LOOKUP1;
        }
    }
}
