package dev.lukebemish.opensesame.test.transformation.groovy.Open

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
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetClass = String.class) Object simpleCoerceTarget() {
        throw new RuntimeException()
    }

    @Test
    void testSimpleCoerceTarget() {
        assertEquals("simpleReturn", simpleCoerceTarget())
    }

    @Open(
            name = "simpleReturn",
            targetClass = Public.class,
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
            name = "simpleReturn",
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetName = "java.lang.String") Object nameCoerceTarget() {
        throw new RuntimeException()
    }

    @Test
    void testNameCoerceTarget() {
        assertEquals("simpleReturn", nameCoerceTarget())
    }

    @Open(
            name = "arrayReturn",
            targetClass = Public.class,
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
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetClass = Integer.class) int boxedTarget() {
        throw new RuntimeException()
    }

    @Test
    void testBoxedTarget() {
        assertEquals(boxedTarget(), 5)
    }

    @Open(
            name = "primitiveReturn",
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetName = "I") Integer unBoxedTargetDesc() {
        throw new RuntimeException()
    }

    @Test
    void testUnBoxedTarget() {
        assertEquals(5, unBoxedTargetDesc())
    }

    @Open(
            name = "simpleArgument",
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static String simpleCoerceArgument(@Coerce(targetClass = String.class) Object value) {
        throw new RuntimeException()
    }

    @Test
    void testSimpleCoerceArgument() {
        Object obj = "test"
        assertEquals("test", simpleCoerceArgument(obj))
    }
    @Open(
            name = "simpleArgument",
            targetClass = Public.class,
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
            name = "simpleArgument",
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static String nameCoerceArgument(@Coerce(targetName = "java.lang.String") Object value) {
        throw new RuntimeException()
    }

    @Test
    void testNameCoerceArgument() {
        Object obj = "test"
        assertEquals("test", nameCoerceArgument(obj))
    }

    @Open(
            name = "arrayArgument",
            targetClass = Public.class,
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
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static String boxedArgument(@Coerce(targetClass = Integer.class) int value) {
        throw new RuntimeException()
    }

    @Test
    void testBoxedArgument() {
        assertEquals("5", boxedArgument(5))
    }

    @Open(
            name = "primitiveArgument",
            targetClass = Public.class,
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
