package dev.lukebemish.opensesame.test.groovy.Open

import dev.lukebemish.opensesame.annotations.Coerce
import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.test.target.Public
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class TestTypes {
    @Open(
            name = 'voidReturn',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static void publicVoidReturn() {
        throw new RuntimeException()
    }

    @Test
    void testVoidReturn() {
        int count = Public.voidReturnCounter
        publicVoidReturn()
        assertEquals(Public.voidReturnCounter, count + 1)
    }

    @Open(
            name = 'primitiveReturn',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static int publicPrimitiveReturn() {
        throw new RuntimeException()
    }

    @Test
    void testPrimitiveReturn() {
        assertEquals(publicPrimitiveReturn(), 5)
    }

    @Open(
            name = 'privateReturn',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetName = 'dev.lukebemish.opensesame.test.target.Public$Private') Object publicPrivateReturn() {
        throw new RuntimeException()
    }

    @Test
    void testPrivateReturn() {
        assertEquals("Private", publicPrivateReturn().toString())
    }

    @Open(
            name = 'arrayReturn',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String[] publicArrayReturn() {
        throw new RuntimeException()
    }

    @Test
    void testArrayReturn() {
        assertArrayEquals(new String[] {"a", "b"}, publicArrayReturn())
    }

    @Open(
            name = 'primitiveArgument',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String publicPrimitiveArgument(int value) {
        throw new RuntimeException()
    }

    @Test
    void testPrimitiveArgument() {
        assertEquals('5', publicPrimitiveArgument(5))
    }

    @Open(
            name = 'privateArgument',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String publicPrivateArgument(@Coerce(targetName = 'dev.lukebemish.opensesame.test.target.Public$Private') Object value) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateArgument() {
        assertEquals("Private1", publicPrivateArgument(TestMisc.privatePrivateConstructor()))
    }

    @Open(
            name = 'arrayArgument',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String publicArrayArgument(String[] value) {
        throw new RuntimeException()
    }

    @Test
    void testArrayArgument() {
        assertEquals("a", publicArrayArgument(new String[]{"a", "b"}))
    }
}
