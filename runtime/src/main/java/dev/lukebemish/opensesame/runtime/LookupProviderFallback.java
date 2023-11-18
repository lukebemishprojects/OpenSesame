package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;

@ApiStatus.Internal
class LookupProviderFallback implements LookupProvider {
    @Override
    public MethodHandles.Lookup openingLookup(MethodHandles.Lookup original, Class<?> target) throws IllegalAccessException {
        return MethodHandles.privateLookupIn(target, original);
    }
}
