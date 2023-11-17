package dev.lukebemish.opensesame.test.javac;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.test.target.ToOpen;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestOpen {
    private static final class PrivateClassProvider implements Function<ClassLoader, Class<?>> {
        @Override
        public Class<?> apply(ClassLoader classLoader) {
            try {
                return Class.forName("dev.lukebemish.opensesame.test.target.ToOpen", false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Open(
            name = "testInstance",
            targetProvider = PrivateClassProvider.class,
            type = Open.Type.SPECIAL
    )
    private static String openerTestPrivateAccess(ToOpen instance) {
        throw new RuntimeException();
    }

    private static int test(int i) {
        return 0;
    }

    @Test
    void testPrivateAccess() {
        ToOpen testPrivate = new ToOpen();
        assertEquals("ran private instance method", openerTestPrivateAccess(testPrivate));
    }

    @Open(
            name = "invoke",
            targetName = "dev.lukebemish.opensesame.test.target.HasPrivateSubclass$PrivateSubclass",
            type = Open.Type.STATIC
    )
    private static String openerTestPrivateClass() {
        throw new RuntimeException();
    }

    @Test
    void testPrivateClass() {
        assertEquals("PrivateSubclass", openerTestPrivateClass());
    }

    @Open(
            name = "<init>",
            targetName = "dev.lukebemish.opensesame.test.target.HasPrivateSubclass$PrivateSubclass",
            type = Open.Type.CONSTRUCT
    )
    private static Object openerTestPrivateCtor(String arg) {
        throw new RuntimeException();
    }

    @Open(
            name = "arg",
            targetName = "dev.lukebemish.opensesame.test.target.HasPrivateSubclass$PrivateSubclass",
            type = Open.Type.GET_INSTANCE
    )
    private static String openerTestPrivateCtorField(Object instance) {
        throw new RuntimeException();
    }

    @Test
    void testPrivateCtor() {
        var object = openerTestPrivateCtor("test");
        assertEquals("test", openerTestPrivateCtorField(object));
    }

    @Open(
            name = "getRuntimeArguments",
            targetName = "jdk.internal.misc.VM",
            type = Open.Type.STATIC
    )
    private static String[] openerTestModuleBreaking() {
        throw new RuntimeException();
    }

    @Test
    void testModuleBreaking() {
        openerTestModuleBreaking();
    }

    static class TestInstance extends ToOpen {
        @Open(
                name = "testInstance",
                targetClass = ToOpen.class,
                type = Open.Type.SPECIAL
        )
        String openTestInstance() {
            throw new RuntimeException();
        }
    }

    @Test
    void testInstance() {
        assertEquals("ran private instance method", new TestInstance().openTestInstance());
    }
}
