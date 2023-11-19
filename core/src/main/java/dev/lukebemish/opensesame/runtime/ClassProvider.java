package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.Nullable;

/**
 * Provides a class given a name and classloader.
 */
public interface ClassProvider {
    /**
     * Provide a class to the {@link OpeningMetafactory} invoking this.
     * @param loader the classloader the caller of the {@link OpeningMetafactory} is using; may be ignored if not needed
     * @param name the name provided in the annotation that constructs the call to this method, or {@code null} if none
     *             was provided
     * @return the class to use
     */
    Class<?> provide(ClassLoader loader, @Nullable String name);
}
