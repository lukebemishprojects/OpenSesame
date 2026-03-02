package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;

@ApiStatus.Internal
interface LookupProvider {
    MethodHandles.Lookup unsafeLookup();
}
