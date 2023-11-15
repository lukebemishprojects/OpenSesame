package dev.lukebemish.opensesame.runtime;

import java.util.function.Function;

public final class ErrorFunction implements Function<ClassLoader, Class<?>> {
    @Override
    public Class<?> apply(ClassLoader classLoader) {
        throw new OpeningException("Could not provide class");
    }
}
