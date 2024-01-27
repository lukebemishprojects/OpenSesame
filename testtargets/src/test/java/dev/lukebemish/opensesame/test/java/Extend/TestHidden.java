package dev.lukebemish.opensesame.test.java.Extend;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestHidden {
    @Extend(
            targetName = "dev.lukebemish.opensesame.test.target.hidden.Hidden",
            unsafe = true
    )
    public interface Extension {
        @Constructor
        static Extension constructor() {
            throw new AssertionError("Constructor not transformed");
        }
    }

    @Open(
            name = "hiddenByModulesPrivateInstance",
            targetName = "dev.lukebemish.opensesame.test.target.hidden.Hidden",
            unsafe = true,
            type = Open.Type.VIRTUAL
    )
    public static String hiddenByModulesPrivateInstance(Object instance) {
        throw new AssertionError("Method not transformed");
    }

    @Test
    void testExtensionHiddenByModules() {
        assertEquals("hiddenByModulesPrivateInstance", hiddenByModulesPrivateInstance(Extension.constructor()));
    }
}
