package dev.lukebemish.opensesame.test.groovy


import dev.lukebemish.opensesame.annotations.groovy.OpenClass
import dev.lukebemish.opensesame.test.target.Public
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
@SuppressWarnings('GroovyAccessibility')
class TestOpenClass {
    @Test
    @OpenClass(Public)
    void testPrivateInstance() {
        assertEquals("privateInstance", new Public().privateInstance())
    }

    @Test
    @OpenClass(Public)
    void testPrivateInstanceOverloaded() {
        assertEquals("privateInstanceOverloaded", new Public().privateInstanceOverloaded())
        assertEquals("privateInstanceOverloaded", ((Public) new Public.PublicSubclass()).privateInstanceOverloaded())
    }

    @Test
    @OpenClass(Public)
    void testProtectedInstance() {
        assertEquals("protectedInstance", new Public().protectedInstance())
    }

    @Test
    @OpenClass(Public)
    void testPackagePrivateInstance() {
        assertEquals("packagePrivateInstance", new Public().packagePrivateInstance())
    }

    @Test
    @OpenClass(Public)
    void testPrivateInstanceField() {
        Public instance = new Public()
        instance.privateInstanceField = 'test'
        assertEquals("test", instance.privateInstanceField)
    }

    @Test
    @OpenClass(Public)
    void testProtectedInstanceField() {
        Public instance = new Public()
        instance.protectedInstanceField = 'test'
        assertEquals("test", instance.protectedInstanceField)
    }

    @Test
    @OpenClass(Public)
    void testPackagePrivateInstanceField() {
        Public instance = new Public()
        instance.packagePrivateInstanceField = 'test'
        assertEquals("test", instance.packagePrivateInstanceField)
    }

    @Test
    @OpenClass(Public)
    void testPrivateFinalInstanceField() {
        assertEquals("privateFinalInstanceField", new Public().privateFinalInstanceField)
    }

    @Test
    @OpenClass(Public)
    void testPrivateStatic() {
        assertEquals("privateStatic", Public.privateStatic())
    }

    @Test
    @OpenClass(Public)
    void testProtectedStatic() {
        assertEquals("protectedStatic", Public.protectedStatic())
    }

    @Test
    @OpenClass(Public)
    void testPackagePrivateStatic() {
        assertEquals("packagePrivateStatic", Public.packagePrivateStatic())
    }

    @Test
    @OpenClass(Public)
    void testPrivateStaticField() {
        Public.privateStaticField = 'test'
        assertEquals("test", Public.privateStaticField)
    }

    @Test
    @OpenClass(Public)
    void testProtectedStaticField() {
        Public.protectedStaticField = 'test'
        assertEquals(Public.protectedStaticField, 'test')
    }

    @Test
    @OpenClass(Public)
    void testPackagePrivateStaticField() {
        Public.packagePrivateStaticField = 'test'
        assertEquals("test", Public.packagePrivateStaticField)
    }

    @Test
    @OpenClass(Public)
    void testPrivateFinalStaticField() {
        assertEquals("privateFinalStaticField", Public.privateFinalStaticField)
    }

    @Test
    @OpenClass(Public.PrivateCtor)
    void testPrivateConstructor() {
        Public.PrivateCtor instance = new Public.PrivateCtor()
        assertEquals("PrivateCtor", instance.toString())
    }

    @Test
    @OpenClass(Public)
    void testVoidReturn() {
        synchronized (Public) {
            int count = Public.voidReturnCounter
            Public.voidReturn()
            assertEquals(Public.voidReturnCounter, count + 1)
        }
    }

    @Test
    @OpenClass(Public)
    void testPrimitiveReturn() {
        assertEquals(Public.primitiveReturn(), 5)
    }

    @Test
    @OpenClass(Public)
    void testArrayReturn() {
        assertArrayEquals(Public.arrayReturn(), new String[] {"a", "b"})
    }

    @Test
    @OpenClass(Public)
    void testPrimitiveArgument() {
        assertEquals('5', Public.primitiveArgument(5))
    }

    @Test
    @OpenClass(Public)
    void testArrayArgument() {
        assertEquals("a", Public.arrayArgument(new String[]{"a", "b"}))
    }
}
