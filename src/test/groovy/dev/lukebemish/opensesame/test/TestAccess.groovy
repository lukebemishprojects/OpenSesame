package dev.lukebemish.opensesame.test

import dev.lukebemish.opensesame.OpenSesame
import dev.lukebemish.opensesame.test.otherpackage.HasPrivateCtor
import dev.lukebemish.opensesame.test.otherpackage.ToOpen
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

@CompileStatic
@SuppressWarnings('GroovyAccessibility')
class TestAccess {
    @Test
    @OpenSesame(ToOpen)
    void testPrivateAccess() {
        ToOpen testPrivate = new ToOpen()
        testPrivate.testInstance()
    }
    @Test
    @OpenSesame(ToOpen)
    void testProtectedAccess() {
        ToOpen testPrivate = new ToOpen()
        testPrivate.testProtectedInstance()
    }

    @Test
    @OpenSesame(ToOpen)
    void testPrivateFieldGet() {
        ToOpen testPrivate = new ToOpen()
        println testPrivate.instance
    }

    @Test
    @OpenSesame(ToOpen)
    void testPrivateStaticFieldGet() {
        println ToOpen.STATIC
    }

    @Test
    @OpenSesame(ToOpen)
    void testPrivateFieldSet() {
        ToOpen testPrivate = new ToOpen()
        testPrivate.instance = 'mutated'
        println testPrivate.instance
    }

    @Test
    @OpenSesame(ToOpen)
    void testPrivateStaticFieldSet() {
        ToOpen.STATIC = 'mutated'
        println ToOpen.STATIC
    }

    @Test
    @OpenSesame(ToOpen)
    void testPrivateStaticAccess() {
        ToOpen.testStatic()
    }

    @Test
    @OpenSesame(ToOpen)
    void testPrivateStaticWithArgAccess() {
        ToOpen.testStaticWithArg('test')
    }

    @Test
    @OpenSesame(HasPrivateCtor)
    void testPrivateCtorAccess() {
        new HasPrivateCtor('test')
    }
}
