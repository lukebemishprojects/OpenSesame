package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.Nullable;

public interface ClassProvider {
    Class<?> provide(ClassLoader loader, @Nullable String name);
}
