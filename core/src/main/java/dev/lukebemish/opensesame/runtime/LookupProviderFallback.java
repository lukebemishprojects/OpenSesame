package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

@ApiStatus.Internal
class LookupProviderFallback implements LookupProvider {
    private static final String EXPOSE_LOOKUP_FIELD = "$$dev$lukebemish$opensesame$$LOOKUP";

    @Override
    public MethodHandles.Lookup openingLookup(MethodHandles.Lookup original, Class<?> target) {
        try {
            Field field = target.getDeclaredField(EXPOSE_LOOKUP_FIELD);
            Object obj = field.get(null);
            if (obj instanceof MethodHandles.Lookup lookup) {
                return MethodHandles.privateLookupIn(target, lookup);
            }
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | NullPointerException ignored) {

        }
        try {
            return MethodHandles.privateLookupIn(target, original);
        } catch (IllegalAccessException e) {
            return original;
        }
    }
}
