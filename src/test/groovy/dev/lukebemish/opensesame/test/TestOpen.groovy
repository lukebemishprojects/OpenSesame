package dev.lukebemish.opensesame.test


import dev.lukebemish.opensesame.Open
import dev.lukebemish.opensesame.test.otherpackage.ToOpen
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

@CompileStatic
class TestOpen {
    @Open(
            name = 'testInstance',
            target = 'dev.lukebemish.opensesame.test.otherpackage.ToOpen',
            type = Open.Type.SPECIAL
    )
    private static String openerTestPrivateAccess(ToOpen instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateAccess() {
        ToOpen testPrivate = new ToOpen()
        assertEquals("ran private instance method", openerTestPrivateAccess(testPrivate))
    }

    @Open(
            name = 'invoke',
            target = 'dev.lukebemish.opensesame.test.otherpackage.HasPrivateSubclass$PrivateSubclass',
            type = Open.Type.STATIC
    )
    private static String openerTestPrivateClass() {
        throw new RuntimeException()
    }

    @Test
    void testPrivateClass() {
        assertEquals("PrivateSubclass", openerTestPrivateClass())
    }

    @Open(
            name = '<init>',
            target = 'dev.lukebemish.opensesame.test.otherpackage.HasPrivateSubclass$PrivateSubclass',
            type = Open.Type.CONSTRUCT
    )
    private static Object openerTestPrivateCtor(String arg) {
        throw new RuntimeException()
    }

    @Open(
            name = 'arg',
            target = 'dev.lukebemish.opensesame.test.otherpackage.HasPrivateSubclass$PrivateSubclass',
            type = Open.Type.GET_INSTANCE
    )
    private static String openerTestPrivateCtorField(Object instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateCtor() {
        var object = openerTestPrivateCtor('test')
        assertEquals('test', openerTestPrivateCtorField(object))
    }

    @Open(
            name = 'getRuntimeArguments',
            target = 'jdk.internal.misc.VM',
            type = Open.Type.STATIC
    )
    private static String[] openerTestModuleBreaking() {
        throw new RuntimeException()
    }

    @Test
    void testModuleBreaking() {
        openerTestModuleBreaking()
    }
}
