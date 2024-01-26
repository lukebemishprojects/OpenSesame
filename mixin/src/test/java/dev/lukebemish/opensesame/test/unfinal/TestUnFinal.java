package dev.lukebemish.opensesame.test.unfinal;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import dev.lukebemish.opensesame.mixin.annotations.UnFinal;
import dev.lukebemish.opensesame.test.target.Final;
import dev.lukebemish.opensesame.test.target.Public;
import org.junit.jupiter.api.Disabled;

// We don't have a way to test this besides looking at the bytecode, at present
@Disabled
public class TestUnFinal {
    @Open(
            name = "privateFinalInstanceField",
            targetClass = Public.class,
            type = Open.Type.SET_STATIC
    )
    @UnFinal
    private static void privateFinalInstanceField(String value) {
        throw new AssertionError("Method not replaced");
    }

    @Extend(
            targetClass = Final.class,
            unsafe = true
    )
    @UnFinal
    public interface FinalExtension {
        @Constructor
        static FinalExtension constructor() {
            throw new AssertionError("Constructor not replaced");
        }
    }

    @Extend(
            targetClass = Public.class,
            unsafe = true
    )
    public interface PublicExtension {
        @Constructor
        static PublicExtension constructor() {
            throw new AssertionError("Constructor not replaced");
        }

        @Overrides(name = "finalMethod")
        @UnFinal
        default String finalMethodOverride() {
            return "not so final now!";
        }
    }
}
