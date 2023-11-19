package dev.lukebemish.opensesame.test.groovy.Open

import dev.lukebemish.opensesame.annotations.Coerce
import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.test.target.Public
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class TestCoerce {
    @Open(
            name = "simpleReturn",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetClass = String) Object simpleCoerceTarget() {
        throw new RuntimeException()
    }

    @Test
    void testSimpleCoerceTarget() {
        assertEquals("simpleReturn", simpleCoerceTarget())
    }
    @Open(
            name = "simpleReturn",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetName = "Ljava/lang/String;") Object descCoerceTarget() {
        throw new RuntimeException()
    }

    @Test
    void testDescCoerceTarget() {
        assertEquals("simpleReturn", descCoerceTarget())
    }

    @Open(
            name = "privateReturn",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetName = 'dev.lukebemish.opensesame.test.target.Public$Private') Object privateCoerceTarget() {
        throw new RuntimeException()
    }

    @Test
    void testPrivateCoerceTarget() {
        assertEquals("Private", privateCoerceTarget().toString())
    }

    @Open(
            name = "arrayReturn",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetName = "[Ljava/lang/String;") Object[] arrayDescCoerceTarget() {
        throw new RuntimeException()
    }

    @Test
    void testArrayDescCoerceTarget() {
        assertArrayEquals(new String[] {"a", "b"}, arrayDescCoerceTarget())
    }

    @Open(
            name = "boxedReturn",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetClass = Integer) int boxedTarget() {
        throw new RuntimeException()
    }

    @Test
    void testBoxedTarget() {
        assertEquals(boxedTarget(), 5)
    }

    @Open(
            name = "primitiveReturn",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetName = "I") Integer unBoxedTargetDesc() {
        throw new RuntimeException()
    }

    @Test
    void testUnBoxedTarget() {
        assertEquals(unBoxedTargetDesc(), 5)
    }

    @Open(
            name = "simpleArgument",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String simpleCoerceArgument(@Coerce(targetClass = String) Object value) {
        throw new RuntimeException()
    }

    @Test
    void testSimpleCoerceArgument() {
        Object obj = "test"
        assertEquals("test", simpleCoerceArgument(obj))
    }
    @Open(
            name = "simpleArgument",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String descCoerceArgument(@Coerce(targetName = "Ljava/lang/String;") Object value) {
        throw new RuntimeException()
    }

    @Test
    void testDescCoerceArgument() {
        Object obj = "test"
        assertEquals("test", descCoerceArgument(obj))
    }

    @Open(
            name = "privateArgument",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String privateCoerceArgument(@Coerce(targetName = 'dev.lukebemish.opensesame.test.target.Public$Private') Object value) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateCoerceArgument() {
        assertEquals("Private1", privateCoerceArgument(TestMisc.privatePrivateConstructor()))
    }

    @Open(
            name = "arrayArgument",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String arrayDescCoerceArgument(@Coerce(targetName = "[Ljava/lang/String;") Object[] value) {
        throw new RuntimeException()
    }

    @Test
    void testArrayDescCoerceArgument() {
        Object[] objs = new String[]{"a", "b"}
        assertEquals("a", arrayDescCoerceArgument(objs))
    }

    @Open(
            name = "boxedArgument",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String boxedArgument(@Coerce(targetClass = Integer) int value) {
        throw new RuntimeException()
    }

    @Test
    void testBoxedArgument() {
        assertEquals("5", boxedArgument(5))
    }

    @Open(
            name = "primitiveArgument",
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String unBoxedArgumentDesc(@Coerce(targetName = "I") Integer value) {
        throw new RuntimeException()
    }

    @Test
    void testUnBoxedArgument() {
        assertEquals("5", unBoxedArgumentDesc(5))
    }
}
