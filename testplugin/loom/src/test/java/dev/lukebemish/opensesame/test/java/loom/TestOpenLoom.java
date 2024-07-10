package dev.lukebemish.opensesame.test.java.loom;

import dev.lukebemish.opensesame.annotations.Open;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestOpenLoom {
    @BeforeAll
    static void init() {
        FakeKnot.init();
    }

    @Open(
            name = "decompose",
            targetClass = ResourceLocation.class,
            type = Open.Type.STATIC
    )
    private static String[] decompose1(String string, char c) {
        throw new RuntimeException();
    }
    @Open(
            name = "decompose",
            targetName = "net.minecraft.resources.ResourceLocation",
            type = Open.Type.STATIC
    )
    private static String[] decompose2(String string, char c) {
        throw new RuntimeException();
    }

    @Open(
            name = "namespace",
            targetName = "net.minecraft.resources.ResourceLocation",
            type = Open.Type.GET_INSTANCE
    )
    private static String getNamespace(ResourceLocation resourceLocation) {
        throw new RuntimeException();
    }

    @Test
    void testStaticClass() {
        assertArrayEquals(new String[]{"a", "b"}, decompose1("a:b", ':'));
    }

    @Test
    void testStaticName() {
        assertArrayEquals(new String[]{"a", "b"}, decompose2("a:b", ':'));
    }

    @Test
    void testGetInstance() {
        assertEquals("a", getNamespace(new ResourceLocation("a:b")));
    }
}
