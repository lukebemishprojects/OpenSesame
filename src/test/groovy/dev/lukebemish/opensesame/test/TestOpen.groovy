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
            targetProvider = { ClassLoader it -> Class.forName('dev.lukebemish.opensesame.test.otherpackage.ToOpen', false, it) },
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
            targetName = 'dev.lukebemish.opensesame.test.otherpackage.HasPrivateSubclass$PrivateSubclass',
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
            targetName = 'dev.lukebemish.opensesame.test.otherpackage.HasPrivateSubclass$PrivateSubclass',
            type = Open.Type.CONSTRUCT
    )
    private static Object openerTestPrivateCtor(String arg) {
        throw new RuntimeException()
    }

    @Open(
            name = 'arg',
            targetName = 'dev.lukebemish.opensesame.test.otherpackage.HasPrivateSubclass$PrivateSubclass',
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
            targetName = 'jdk.internal.misc.VM',
            type = Open.Type.STATIC
    )
    private static String[] openerTestModuleBreaking() {
        throw new RuntimeException()
    }

    @Test
    void testModuleBreaking() {
        openerTestModuleBreaking()
    }

    static class TestInstance extends ToOpen {
        @Open(
                name = 'testInstance',
                targetClass = ToOpen,
                type = Open.Type.SPECIAL
        )
        String openTestInstance() {
            throw new RuntimeException()
        }
    }

    @Test
    void testInstance() {
        assertEquals("ran private instance method", new TestInstance().openTestInstance())
    }
}
