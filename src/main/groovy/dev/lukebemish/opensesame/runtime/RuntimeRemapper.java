package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.Nullable;

public interface RuntimeRemapper {
    @Nullable String remapMethodName(Class<?> parent, String name, Class<?>[] args);
}
