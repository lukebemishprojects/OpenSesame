package dev.lukebemish.opensesame.runtime;

import java.lang.invoke.MethodHandles;

interface LookupProvider {
    MethodHandles.Lookup openingLookup(Class<?> target) throws IllegalAccessException;
}
