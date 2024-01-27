package dev.lukebemish.opensesame.test.java.Open;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.test.target.BigLVT;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestBigLVT {
    @Open(
            name = "longReturn",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static long longReturn() {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "longArgument",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static String longArgument(long l) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "longArgument",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static String longArgument(long l, long l2) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "longMixedArguments",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static String longMixedArguments(long l, String s, long l2) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "longCenterArgument",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static String longCenterArgument(String s, long l, String s2) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "doubleReturn",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static double doubleReturn() {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "doubleArgument",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static String doubleArgument(double d) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "doubleArgument",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static String doubleArgument(double d, double d2) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "doubleMixedArguments",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static String doubleMixedArguments(double d, String s, double d2) {
        throw new UnsupportedOperationException();
    }

    @Open(
            name = "doubleCenterArgument",
            targetClass = BigLVT.class,
            type = Open.Type.STATIC
    )
    private static String doubleCenterArgument(String s, double d, String s2) {
        throw new UnsupportedOperationException();
    }

    @Test
    void testLongReturn() {
        assertEquals(3L, longReturn());
    }

    @Test
    void testLongArgument() {
        assertEquals("3", longArgument(3L));
    }

    @Test
    void testLongArgument2() {
        assertEquals("34", longArgument(3L, 4L));
    }

    @Test
    void testLongMixedArguments() {
        assertEquals("345", longMixedArguments(3L, "4", 5L));
    }

    @Test
    void testLongCenterArgument() {
        assertEquals("345", longCenterArgument("3", 4L, "5"));
    }

    @Test
    void testDoubleReturn() {
        assertEquals(3.0, doubleReturn());
    }

    @Test
    void testDoubleArgument() {
        assertEquals("3", doubleArgument(3.0));
    }

    @Test
    void testDoubleArgument2() {
        assertEquals("34", doubleArgument(3.0, 4.0));
    }

    @Test
    void testDoubleMixedArguments() {
        assertEquals("345", doubleMixedArguments(3.0, "4", 5.0));
    }

    @Test
    void testDoubleCenterArgument() {
        assertEquals("345", doubleCenterArgument("3", 4.0, "5"));
    }
}
