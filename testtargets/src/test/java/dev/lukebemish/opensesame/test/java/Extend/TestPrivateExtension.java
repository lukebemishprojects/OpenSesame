package dev.lukebemish.opensesame.test.java.Extend;

import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPrivateExtension {
    @Extend(targetName = "dev.lukebemish.opensesame.test.target.Public$Private")
    public interface Extension {
        @Constructor
        static Extension constructor() {
            throw new AssertionError("Constructor not transformed");
        }

        @Overrides(name = "toString")
        default String toStringImplementation() {
            return "Extension";
        }
    }

    @Test
    void testPrivateExtension() {
        assertEquals("Extension", Extension.constructor().toString());
    }
}
