package dev.lukebemish.opensesame.test.target;

public sealed interface SealedClass {
    record Allowed() implements SealedClass {}
}
