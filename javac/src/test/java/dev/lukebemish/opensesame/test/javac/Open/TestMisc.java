package dev.lukebemish.opensesame.test.javac.Open;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.test.target.Public;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestMisc {
    @Open(
            name = "privateInstanceOverloaded",
            targetClass = Public.class,
            type = Open.Type.VIRTUAL
    )
    private static String publicPrivateInstanceOverloaded(Public instance) {
        throw new RuntimeException();
    }

    @Test
    void testPrivateInstanceOverloaded() {
        assertEquals("privateInstanceOverloaded", publicPrivateInstanceOverloaded(new Public()));
        assertEquals("privateInstanceOverloaded", publicPrivateInstanceOverloaded(new Public.PublicSubclass()));
    }

    @Open(
            targetName = "dev.lukebemish.opensesame.test.target.Public$Private",
            type = Open.Type.CONSTRUCT
    )
    static Object privatePrivateConstructor() {
        throw new RuntimeException();
    }

    @Open(
            name = "privateInstance",
            targetName = "dev.lukebemish.opensesame.test.target.Public$Private",
            type = Open.Type.VIRTUAL
    )
    private static String privatePrivateInstance(Object instance) {
        throw new RuntimeException();
    }

    @Test
    void testPrivateClass() {
        Object instance = privatePrivateConstructor();
        assertEquals("Private", instance.toString());
        assertEquals("privateInstance", privatePrivateInstance(instance));
    }

    @Open(
            targetName = "dev.lukebemish.opensesame.test.target.Public$PrivateCtor",
            type = Open.Type.CONSTRUCT
    )
    private static Public.PrivateCtor privateConstructor() {
        throw new RuntimeException();
    }

    @Test
    void testPrivateConstructor() {
        Public.PrivateCtor instance = privateConstructor();
        assertEquals("PrivateCtor", instance.toString());
    }

    @Open(
            targetName = "dev.lukebemish.opensesame.test.target.PackagePrivate",
            type = Open.Type.CONSTRUCT
    )
    private static Object packagePrivateConstructor() {
        throw new RuntimeException();
    }

    @Open(
            name = "privateInstance",
            targetName = "dev.lukebemish.opensesame.test.target.PackagePrivate",
            type = Open.Type.VIRTUAL
    )
    private static String packagePrivateInstance(Object instance) {
        throw new RuntimeException();
    }

    @Test
    void testPackagePrivateClass() {
        Object instance = packagePrivateConstructor();
        assertEquals("PackagePrivate", instance.toString());
        assertEquals("privateInstance", packagePrivateInstance(instance));
    }

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

    @Open(
            targetName = "dev.lukebemish.opensesame.test.target.Public$Private",
            type = Open.Type.ARRAY
    )
    private static Object[] privateArrayConstructor(int i) {
        throw new RuntimeException();
    }

    @Test
    void testPrivateArrayConstructor() {
        Object[] array = privateArrayConstructor(2);
        assertEquals(2, array.length);
        array[0] = privatePrivateConstructor();
        assertEquals("Private", array[0].toString());
        assertThrows(ArrayStoreException.class, () -> {
            array[1] = "stuff";
        });
    }
}
