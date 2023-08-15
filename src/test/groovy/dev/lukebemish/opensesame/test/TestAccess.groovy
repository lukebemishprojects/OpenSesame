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
