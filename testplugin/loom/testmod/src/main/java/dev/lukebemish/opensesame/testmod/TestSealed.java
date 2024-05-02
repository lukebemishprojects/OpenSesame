package dev.lukebemish.opensesame.testmod;

public sealed interface TestSealed {
    record Allowed() implements TestSealed {}
}
