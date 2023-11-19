package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.Nullable;

/**
 * An implementation of {@link ClassProvider} that always throws an exception.
 */
public final class ErrorProvider implements ClassProvider {
    @Override
    public Class<?> provide(ClassLoader classLoader, @Nullable String targetName) {
        throw new OpeningException("Could not provide class");
    }
}
