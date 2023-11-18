package dev.lukebemish.opensesame.test.javac.Open;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.runtime.ClassProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestProvider {
    private static class SimpleProvider implements ClassProvider {
        @Override
        public Class<?> provide(ClassLoader loader, String name) {
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Open(
            name = "privateStatic",
            targetName = "dev.lukebemish.opensesame.test.target.Public",
            targetProvider = SimpleProvider.class,
            type = Open.Type.STATIC
    )
    private static String simpleProviderTarget() {
        throw new RuntimeException();
    }

    @Test
    void testSimpleProviderTarget() {
        assertEquals("privateStatic", simpleProviderTarget());
    }
}
