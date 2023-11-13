package dev.lukebemish.opensesame.runtime;

import java.lang.invoke.MethodHandles;

class LookupProviderFallback implements LookupProvider {
    @Override
    public MethodHandles.Lookup openingLookup(Class<?> target) throws IllegalAccessException {
        return MethodHandles.privateLookupIn(target, MethodHandles.lookup());
    }
}
