package dev.lukebemish.opensesame.test.java.Open;

import dev.lukebemish.opensesame.annotations.Open;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("modules")
public class TestModules {
    @Open(
            name = "hiddenByModules",
            targetName = "dev.lukebemish.opensesame.test.target.hidden.Hidden",
            type = Open.Type.STATIC,
            unsafe = true
    )
    private static String hiddenByModules() {
        throw new RuntimeException();
    }

    @Open(
            name = "hiddenByModulesPrivate",
            targetName = "dev.lukebemish.opensesame.test.target.hidden.Hidden",
            type = Open.Type.STATIC,
            unsafe = true
    )
    private static String hiddenByModulesPrivate() {
        throw new RuntimeException();
    }

    @Test
    void testModuleBreaking() {
        assertEquals("hiddenByModules", hiddenByModules());
        assertEquals("hiddenByModulesPrivate", hiddenByModulesPrivate());
        assertThrows(IllegalAccessException.class, () -> {
            Class<?> hidden = Class.forName("dev.lukebemish.opensesame.test.target.hidden.Hidden");
            hidden.getDeclaredMethod("hiddenByModules").invoke(null);
        });
    }
}
