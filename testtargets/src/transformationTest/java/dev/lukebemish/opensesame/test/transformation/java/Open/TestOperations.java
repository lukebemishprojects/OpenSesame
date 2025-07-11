package dev.lukebemish.opensesame.test.transformation.java.Open;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.test.target.Public;
import dev.lukebemish.opensesame.test.transformation.java.TransformerAwareTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestOperations implements TransformerAwareTest {
    @Open(
            name = "privateStatic",
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static String publicPrivateStatic() {
        throw new RuntimeException();
    }

    @Test
    void testStatic() {
        assertEquals("privateStatic", publicPrivateStatic());
    }

    @Open(
            name = "privateStatic",
            targetClass = Public.class,
            type = Open.Type.STATIC,
            unsafe = true
    )
    private static String publicPrivateStaticUnsafe() {
        throw new RuntimeException();
    }

    @Test
    void testStaticWithUnsafe() {
        assertEquals("privateStatic", publicPrivateStaticUnsafe());
    }

    @Open(
            name = "privateStaticField",
            targetClass = Public.class,
            type = Open.Type.GET_STATIC
    )
    private static String publicPrivateStaticField() {
        throw new RuntimeException();
    }

    @Open(
            name = "privateStaticField",
            targetClass = Public.class,
            type = Open.Type.SET_STATIC
    )
    private static void publicPrivateStaticField(String value) {
        throw new RuntimeException();
    }

    @Test
    void testFieldSetGet() {
        publicPrivateStaticField("test");
        assertEquals("test", publicPrivateStaticField());
    }

    @Open(
            name = "privateInstance",
            targetClass = Public.class,
            type = Open.Type.VIRTUAL
    )
    private static String publicPrivateVirtual(Public instance) {
        throw new RuntimeException();
    }

    @Test
    void testVirtual() {
        assertEquals("privateInstance", publicPrivateVirtual(new Public()));
    }

    @Open(
            name = "privateInstanceField",
            targetClass = Public.class,
            type = Open.Type.GET_INSTANCE
    )
    private static String publicPrivateInstanceField(Public instance) {
        throw new RuntimeException();
    }

    @Open(
            name = "privateInstanceField",
            targetClass = Public.class,
            type = Open.Type.SET_INSTANCE
    )
    private static void publicPrivateInstanceField(Public instance, String value) {
        throw new RuntimeException();
    }

    @Test
    void testInstanceSetGen() {
        var instance = new Public();
        publicPrivateInstanceField(instance, "test");
        assertEquals("test", publicPrivateInstanceField(instance));
    }

    @Open(
            targetClass = Public.class,
            type = Open.Type.CONSTRUCT
    )
    private static Public publicConstructor(String string) {
        throw new RuntimeException();
    }

    @Test
    void testConstructor() {
        var instance = publicConstructor("test");
        assertEquals("test", instance.publicInstanceField);
    }

    @Open(
            targetClass = Public.class,
            type = Open.Type.ARRAY
    )
    private static Public[] publicArray(int size) {
        throw new RuntimeException();
    }
    
    @Test
    void testArray() {
        Public[] array = publicArray(5);
        assertEquals(5, array.length);
    }
}
