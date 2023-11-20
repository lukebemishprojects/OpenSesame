package dev.lukebemish.opensesame.test.java.Open;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.test.target.Public;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStatic {
    @Open(
            name = "privateStatic",
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static String publicPrivateStatic() {
        throw new RuntimeException();
    }

    @Test
    void testPrivateStatic() {
        assertEquals("privateStatic", publicPrivateStatic());
    }

    @Open(
            name = "protectedStatic",
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static String publicProtectedStatic() {
        throw new RuntimeException();
    }

    @Test
    void testProtectedStatic() {
        assertEquals("protectedStatic", publicProtectedStatic());
    }

    @Open(
            name = "packagePrivateStatic",
            targetClass = Public.class,
            type = Open.Type.STATIC
    )
    private static String publicPackagePrivateStatic() {
        throw new RuntimeException();
    }

    @Test
    void testPackagePrivateStatic() {
        assertEquals("packagePrivateStatic", publicPackagePrivateStatic());
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
    void testPrivateStaticField() {
        publicPrivateStaticField("test");
        assertEquals("test", publicPrivateStaticField());
    }

    @Open(
            name = "protectedStaticField",
            targetClass = Public.class,
            type = Open.Type.GET_STATIC
    )
    private static String publicProtectedStaticField() {
        throw new RuntimeException();
    }

    @Open(
            name = "protectedStaticField",
            targetClass = Public.class,
            type = Open.Type.SET_STATIC
    )
    private static void publicProtectedStaticField(String value) {
        throw new RuntimeException();
    }

    @Test
    void testProtectedStaticField() {
        publicProtectedStaticField("test");
        assertEquals("test", publicProtectedStaticField());
    }

    @Open(
            name = "packagePrivateStaticField",
            targetClass = Public.class,
            type = Open.Type.GET_STATIC
    )
    private static String publicPackagePrivateStaticField() {
        throw new RuntimeException();
    }

    @Open(
            name = "packagePrivateStaticField",
            targetClass = Public.class,
            type = Open.Type.SET_STATIC
    )
    private static void publicPackagePrivateStaticField(String value) {
        throw new RuntimeException();
    }

    @Test
    void testPackagePrivateStaticField() {
        publicPackagePrivateStaticField("test");
        assertEquals("test", publicPackagePrivateStaticField());
    }

    @Open(
            name = "privateFinalStaticField",
            targetClass = Public.class,
            type = Open.Type.GET_STATIC
    )
    private static String publicPrivateFinalStaticField() {
        throw new RuntimeException();
    }

    @Test
    void testPrivateFinalStaticField() {
        assertEquals("privateFinalStaticField", publicPrivateFinalStaticField());
    }
}
