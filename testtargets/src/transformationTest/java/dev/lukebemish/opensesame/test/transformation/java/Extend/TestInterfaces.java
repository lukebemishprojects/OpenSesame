package dev.lukebemish.opensesame.test.transformation.java.Extend;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import dev.lukebemish.opensesame.test.transformation.java.TransformerAwareTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInterfaces implements TransformerAwareTest {
    @Extend(targetName = "dev.lukebemish.opensesame.test.target.PackagePrivateInterface", unsafe = true)
    public interface Extension {
        @Constructor
        static Extension constructor() {
            throw new AssertionError("Constructor not transformed");
        }

        @Overrides(value = "value")
        default String valueImplementation() {
            return "Extension";
        }
    }

    @Open(
            name = "value",
            targetName = "dev.lukebemish.opensesame.test.target.PackagePrivateInterface",
            type = Open.Type.VIRTUAL
    )
    public static String value(Object instance) {
        throw new AssertionError("Method not transformed");
    }

    @Test
    void testExtension() {
        assertEquals("Extension", value(Extension.constructor()));
    }
}
