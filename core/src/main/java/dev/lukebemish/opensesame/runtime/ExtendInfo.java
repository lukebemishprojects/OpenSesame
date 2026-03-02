package dev.lukebemish.opensesame.runtime;

import java.lang.invoke.MethodType;

public record ExtendInfo(
        Class<?> target,
        Class<?> extension,
        Field[] fields,
        Constructor[] constructors,
        Override[] overrides
) {
    public record Field(
            Class<?> type,
            String name,
            boolean isFinal
    ) {}
    public record Constructor(
            MethodType implType,
            MethodType superType,
            String[] fields,
            int[] fieldsIndices
    ) {}
    public record Override(
            MethodType implType,
            MethodType superType,
            String implName,
            String superName
    ) {}
}
