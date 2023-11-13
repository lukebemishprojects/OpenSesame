package dev.lukebemish.opensesame.test

import dev.lukebemish.opensesame.OpenClass
import dev.lukebemish.opensesame.test.otherpackage.HasPrivateCtor
import dev.lukebemish.opensesame.test.otherpackage.ToOpen
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
@SuppressWarnings('GroovyAccessibility')
class TestOpenClass {
    @Test
    @OpenClass(ToOpen)
    void testPrivateAccess() {
        ToOpen testPrivate = new ToOpen()
        assertEquals("ran private instance method", testPrivate.testInstance())
    }
    @Test
    @OpenClass(ToOpen)
    void testProtectedAccess() {
        ToOpen testPrivate = new ToOpen()
        assertEquals("ran protected instance method", testPrivate.testProtectedInstance())
    }

    @Test
    @OpenClass(ToOpen)
    void testPrivateField() {
        ToOpen testPrivate = new ToOpen()
        assertEquals("private instance field", testPrivate.instance)
        testPrivate.instance = 'mutated'
        assertEquals("mutated", testPrivate.instance)
    }

    @Test
    @OpenClass(ToOpen)
    void testPrivateStaticField() {
        assertEquals("private static field", ToOpen.STATIC)
        ToOpen.STATIC = 'mutated'
        assertEquals("mutated", ToOpen.STATIC)
    }

    @Test
    @OpenClass(ToOpen)
    void testPrivateStaticAccess() {
        assertEquals("ran private static method", ToOpen.testStatic())
    }

    @Test
    @OpenClass(ToOpen)
    void testPrivateStaticWithArgAccess() {
        assertEquals("ran private instance method with arg: test", ToOpen.testStaticWithArg('test'))
    }

    @Test
    @OpenClass(HasPrivateCtor)
    void testPrivateCtorAccess() {
        var object = new HasPrivateCtor('test')
        assertEquals('test', object.arg)
    }
}
