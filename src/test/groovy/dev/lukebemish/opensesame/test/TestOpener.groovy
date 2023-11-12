package dev.lukebemish.opensesame.test

import dev.lukebemish.opensesame.Opener
import dev.lukebemish.opensesame.test.otherpackage.ToOpen
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class TestOpener {
    @Opener(
            name = 'testInstance',
            target = 'dev.lukebemish.opensesame.test.otherpackage.ToOpen',
            desc = '()Ljava/lang/String;',
            type = Opener.Type.SPECIAL
    )
    private static openerTestPrivateAccess(ToOpen instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateAccess() {
        ToOpen testPrivate = new ToOpen()
        assertEquals("ran private instance method", openerTestPrivateAccess(testPrivate))
    }

    @Opener(
            name = 'invoke',
            target = 'dev.lukebemish.opensesame.test.otherpackage.HasPrivateSubclass$PrivateSubclass',
            desc = '()Ljava/lang/String;',
            type = Opener.Type.STATIC
    )
    private static openerTestPrivateClass() {
        throw new RuntimeException()
    }

    @Test
    void testPrivateClass() {
        assertEquals("PrivateSubclass", openerTestPrivateClass())
    }

    @Opener(
            name = '<init>',
            target = 'dev.lukebemish.opensesame.test.otherpackage.HasPrivateSubclass$PrivateSubclass',
            desc = '(Ljava/lang/String;)Ldev/lukebemish/opensesame/test/otherpackage/HasPrivateSubclass$PrivateSubclass;',
            type = Opener.Type.CONSTRUCT
    )
    private static Object openerTestPrivateCtor(String arg) {
        throw new RuntimeException()
    }

    @Opener(
            name = 'arg',
            target = 'dev.lukebemish.opensesame.test.otherpackage.HasPrivateSubclass$PrivateSubclass',
            desc = 'Ljava/lang/String;',
            type = Opener.Type.GET_INSTANCE
    )
    private static String openerTestPrivateCtorField(Object instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateCtor() {
        var object = openerTestPrivateCtor('test')
        assertEquals('test', openerTestPrivateCtorField(object))
    }
}
