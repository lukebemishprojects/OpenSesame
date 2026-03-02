package dev.lukebemish.opensesame.runtime;

import dev.lukebemish.opensesame.annotations.Open;

import java.lang.invoke.MethodType;

public record OpenInfo(
        Class<?> target,
        MethodType targetType,
        String name,
        Open.Type type
) {
}
