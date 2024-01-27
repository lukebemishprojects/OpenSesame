package dev.lukebemish.opensesame.test.java.Extend;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Field;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestPrivate {
    @Extend(targetName = "dev.lukebemish.opensesame.test.target.Public$Private", unsafe = true)
    public interface Extension {
        @Constructor
        static Extension constructor() {
            throw new AssertionError("Constructor not transformed");
        }

        @Overrides(value = "toString")
        default String toStringImplementation() {
            return "Extension";
        }
    }

    @Extend(targetName = "dev.lukebemish.opensesame.test.java.samemoduletarget.Public$Private", unsafe = false)
    public interface ExtensionSafe {
        @Constructor
        static ExtensionSafe constructor() {
            throw new AssertionError("Constructor not transformed");
        }

        @Overrides(value = "toString")
        default String toStringImplementation() {
            return "Extension";
        }
    }

    @Extend(targetName = "dev.lukebemish.opensesame.test.target.Public$Private", unsafe = true)
    public interface ExtensionFields {
        @Constructor
        static ExtensionFields constructor(@Field(value = "field") @Field.Final String field) {
            throw new AssertionError("Constructor not transformed");
        }

        @Constructor
        static ExtensionFields constructor(@Field(value = "field") @Field.Final String field, @Field(value = "field2") String field2) {
            throw new AssertionError("Constructor not transformed");
        }

        @Field(value = "field")
        String getField();

        @Field(value = "field2")
        String getField2();

        @Field(value = "field2")
        void setField2(String field2);

        @Overrides(value = "toString")
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
        var instance = ExtensionFields.constructor("stuff");
        assertEquals("stuff", instance.toString());
        assertNull(instance.getField2());
        instance.setField2("stuff2");
        assertEquals("stuff2", instance.getField2());
        instance = ExtensionFields.constructor("stuff", "stuff2");
        assertEquals("stuff", instance.toString());
        assertEquals("stuff2", instance.getField2());
    }
}
