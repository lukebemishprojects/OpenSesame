package dev.lukebemish.opensesame.test.java.Extend;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Field;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestPrivateExtension {
    @Extend(targetName = "dev.lukebemish.opensesame.test.target.Public$Private", unsafe = true)
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

    @Extend(targetName = "dev.lukebemish.opensesame.test.java.samemoduletarget.Public$Private")
    public interface ExtensionSafe {
        @Constructor
        static ExtensionSafe constructor() {
            throw new AssertionError("Constructor not transformed");
        }

        @Overrides(name = "toString")
        default String toStringImplementation() {
            return "Extension";
        }
    }

    @Extend(targetName = "dev.lukebemish.opensesame.test.target.Public$Private", unsafe = true)
    public interface ExtensionFields {
        @Constructor
        static ExtensionFields constructor(@Field(name = "field") @Field.Final String field) {
            throw new AssertionError("Constructor not transformed");
        }

        @Field(name = "field")
        String getField();

        @Overrides(name = "toString")
        default String toStringImplementation() {
            return getField();
        }
    }

    @Open(
            name = "privateInstance",
            targetName = "dev.lukebemish.opensesame.test.target.Public$Private",
            type = Open.Type.VIRTUAL
    )
    private static String privateInstanceInvoker(Extension instance) {
        throw new RuntimeException();
    }

    @Test
    void testPrivateExtension() {
        assertEquals("Extension", Extension.constructor().toString());
        assertEquals("privateInstance", privateInstanceInvoker(Extension.constructor()));
    }

    @Test
    void testPrivateExtensionSafe() {
        assertEquals("Extension", ExtensionSafe.constructor().toString());
    }

    @Test
    void testPrivateExtensionField() {
        assertEquals("stuff", ExtensionFields.constructor("stuff").toString());
    }
}
