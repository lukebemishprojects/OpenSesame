package dev.lukebemish.opensesame.test

import dev.lukebemish.opensesame.OpenSesame
import org.junit.jupiter.api.Test

class TestAccess {
    @Test
    @OpenSesame(TestPrivate)
    void testPrivateAccess() {
        TestPrivate testPrivate = new TestPrivate()
        testPrivate.testInstance()
    }

    @Test
    @OpenSesame(TestPrivate)
    void testPrivateFieldGet() {
        TestPrivate testPrivate = new TestPrivate()
        println testPrivate.instance
    }

    @Test
    @OpenSesame(TestPrivate)
    void testPrivateStaticFieldGet() {
        println TestPrivate.STATIC
    }

    @Test
    @OpenSesame(TestPrivate)
    void testPrivateFieldSet() {
        TestPrivate testPrivate = new TestPrivate()
        testPrivate.instance = 'mutated'
        println testPrivate.instance
    }

    @Test
    @OpenSesame(TestPrivate)
    void testPrivateStaticFieldSet() {
        TestPrivate.STATIC = 'mutated'
        println TestPrivate.STATIC
    }

    @Test
    @OpenSesame(TestPrivate)
    void testPrivateStaticAccess() {
        TestPrivate.testStatic()
    }

    @Test
    @OpenSesame(TestPrivate)
    void testPrivateStaticWithArgAccess() {
        TestPrivate.testStaticWithArg('test')
    }

    @Test
    @OpenSesame(TestPrivateCtor)
    void testPrivateCtorAccess() {
        new TestPrivateCtor('test')
    }
}
