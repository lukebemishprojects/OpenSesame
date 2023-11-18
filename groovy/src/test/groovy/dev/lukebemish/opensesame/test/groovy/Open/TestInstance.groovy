package dev.lukebemish.opensesame.test.groovy.Open

import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.test.target.Public
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class TestInstance {
    @Open(
            name = 'privateInstance',
            targetClass = Public,
            type = Open.Type.VIRTUAL
    )
    private static String publicPrivateInstance(Public instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateInstance() {
        assertEquals("privateInstance", publicPrivateInstance(new Public()))
    }

    @Open(
            name = 'protectedInstance',
            targetClass = Public,
            type = Open.Type.VIRTUAL
    )
    private static String publicProtectedInstance(Public instance) {
        throw new RuntimeException()
    }

    @Test
    void testProtectedInstance() {
        assertEquals("protectedInstance", publicProtectedInstance(new Public()))
    }

    @Open(
            name = 'packagePrivateInstance',
            targetClass = Public,
            type = Open.Type.VIRTUAL
    )
    private static String publicPackagePrivateInstance(Public instance) {
        throw new RuntimeException()
    }

    @Test
    void testPackagePrivateInstance() {
        assertEquals("packagePrivateInstance", publicPackagePrivateInstance(new Public()))
    }

    @Open(
            name = 'privateInstanceField',
            targetClass = Public,
            type = Open.Type.GET_INSTANCE
    )
    private static String publicPrivateInstanceField(Public instance) {
        throw new RuntimeException()
    }

    @Open(
            name = 'privateInstanceField',
            targetClass = Public,
            type = Open.Type.SET_INSTANCE
    )
    private static void publicPrivateInstanceField(Public instance, String value) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateInstanceField() {
        var instance = new Public()
        publicPrivateInstanceField(instance, "test")
        assertEquals("test", publicPrivateInstanceField(instance))
    }

    @Open(
            name = 'protectedInstanceField',
            targetClass = Public,
            type = Open.Type.GET_INSTANCE
    )
    private static String publicProtectedInstanceField(Public instance) {
        throw new RuntimeException()
    }

    @Open(
            name = 'protectedInstanceField',
            targetClass = Public,
            type = Open.Type.SET_INSTANCE
    )
    private static void publicProtectedInstanceField(Public instance, String value) {
        throw new RuntimeException()
    }

    @Test
    void testProtectedInstanceField() {
        var instance = new Public()
        publicProtectedInstanceField(instance, "test")
        assertEquals("test", publicProtectedInstanceField(instance))
    }

    @Open(
            name = 'packagePrivateInstanceField',
            targetClass = Public,
            type = Open.Type.GET_INSTANCE
    )
    private static String publicPackagePrivateInstanceField(Public instance) {
        throw new RuntimeException()
    }

    @Open(
            name = 'packagePrivateInstanceField',
            targetClass = Public,
            type = Open.Type.SET_INSTANCE
    )
    private static void publicPackagePrivateInstanceField(Public instance, String value) {
        throw new RuntimeException()
    }

    @Test
    void testPackagePrivateInstanceField() {
        var instance = new Public()
        publicPackagePrivateInstanceField(instance, "test")
        assertEquals("test", publicPackagePrivateInstanceField(instance))
    }

    @Open(
            name = 'privateFinalInstanceField',
            targetClass = Public,
            type = Open.Type.GET_INSTANCE
    )
    private static String publicPrivateFinalInstanceField(Public instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateFinalInstanceField() {
        assertEquals("privateFinalInstanceField", publicPrivateFinalInstanceField(new Public()))
    }
}
