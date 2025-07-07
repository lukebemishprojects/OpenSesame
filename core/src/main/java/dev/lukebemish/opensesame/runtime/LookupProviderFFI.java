package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;

@ApiStatus.Internal
class LookupProviderFFI implements LookupProvider {
    LookupProviderFFI() {
        throw wrongVersion();
    }

    @Override
    public MethodHandles.Lookup openingLookup(MethodHandles.Lookup original, Class<?> target) {
        throw wrongVersion();
    }
    
    private static RuntimeException wrongVersion() {
        return new RuntimeException("OpenSesame FFI-based lookup provider is not implemented on this java version.");
    }
}
