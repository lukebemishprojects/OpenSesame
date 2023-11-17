package dev.lukebemish.opensesame.test.groovy

import dev.lukebemish.opensesame.annotations.Coerce
import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.test.target.Public
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

@CompileStatic
class TestOpen {
    @Open(
            name = 'privateInstance',
            targetClass = Public,
            type = Open.Type.VIRTUAL
    )
    private static String publicPrivateInstance(Public instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateInstance() {
        assertEquals("privateInstance", publicPrivateInstance(new Public()))
    }

    @Open(
            name = 'privateInstanceOverloaded',
            targetClass = Public,
            type = Open.Type.VIRTUAL
    )
    private static String publicPrivateInstanceOverloaded(Public instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateInstanceOverloaded() {
        assertEquals("privateInstanceOverloaded", publicPrivateInstanceOverloaded(new Public()))
        assertEquals("privateInstanceOverloaded", publicPrivateInstanceOverloaded(new Public.PublicSubclass()))
    }

    @Open(
            name = 'protectedInstance',
            targetClass = Public,
            type = Open.Type.VIRTUAL
    )
    private static String publicProtectedInstance(Public instance) {
        throw new RuntimeException()
    }

    @Test
    void testProtectedInstance() {
        assertEquals("protectedInstance", publicProtectedInstance(new Public()))
    }

    @Open(
            name = 'packagePrivateInstance',
            targetClass = Public,
            type = Open.Type.VIRTUAL
    )
    private static String publicPackagePrivateInstance(Public instance) {
        throw new RuntimeException()
    }

    @Test
    void testPackagePrivateInstance() {
        assertEquals("packagePrivateInstance", publicPackagePrivateInstance(new Public()))
    }

    @Open(
            name = 'privateInstanceField',
            targetClass = Public,
            type = Open.Type.GET_INSTANCE
    )
    private static String publicPrivateInstanceField(Public instance) {
        throw new RuntimeException()
    }

    @Open(
            name = 'privateInstanceField',
            targetClass = Public,
            type = Open.Type.SET_INSTANCE
    )
    private static void publicPrivateInstanceField(Public instance, String value) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateInstanceField() {
        var instance = new Public()
        publicPrivateInstanceField(instance, "test")
        assertEquals("test", publicPrivateInstanceField(instance))
    }

    @Open(
            name = 'protectedInstanceField',
            targetClass = Public,
            type = Open.Type.GET_INSTANCE
    )
    private static String publicProtectedInstanceField(Public instance) {
        throw new RuntimeException()
    }

    @Open(
            name = 'protectedInstanceField',
            targetClass = Public,
            type = Open.Type.SET_INSTANCE
    )
    private static void publicProtectedInstanceField(Public instance, String value) {
        throw new RuntimeException()
    }

    @Test
    void testProtectedInstanceField() {
        var instance = new Public()
        publicProtectedInstanceField(instance, "test")
        assertEquals("test", publicProtectedInstanceField(instance))
    }

    @Open(
            name = 'packagePrivateInstanceField',
            targetClass = Public,
            type = Open.Type.GET_INSTANCE
    )
    private static String publicPackagePrivateInstanceField(Public instance) {
        throw new RuntimeException()
    }

    @Open(
            name = 'packagePrivateInstanceField',
            targetClass = Public,
            type = Open.Type.SET_INSTANCE
    )
    private static void publicPackagePrivateInstanceField(Public instance, String value) {
        throw new RuntimeException()
    }

    @Test
    void testPackagePrivateInstanceField() {
        var instance = new Public()
        publicPackagePrivateInstanceField(instance, "test")
        assertEquals("test", publicPackagePrivateInstanceField(instance))
    }

    @Open(
            name = 'privateFinalInstanceField',
            targetClass = Public,
            type = Open.Type.GET_INSTANCE
    )
    private static String publicPrivateFinalInstanceField(Public instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateFinalInstanceField() {
        assertEquals("privateFinalInstanceField", publicPrivateFinalInstanceField(new Public()))
    }

    @Open(
            name = 'privateStatic',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String publicPrivateStatic() {
        throw new RuntimeException()
    }

    @Test
    void testPrivateStatic() {
        assertEquals("privateStatic", publicPrivateStatic())
    }

    @Open(
            name = 'protectedStatic',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String publicProtectedStatic() {
        throw new RuntimeException()
    }

    @Test
    void testProtectedStatic() {
        assertEquals("protectedStatic", publicProtectedStatic())
    }

    @Open(
            name = 'packagePrivateStatic',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String publicPackagePrivateStatic() {
        throw new RuntimeException()
    }

    @Test
    void testPackagePrivateStatic() {
        assertEquals("packagePrivateStatic", publicPackagePrivateStatic())
    }

    @Open(
            name = 'privateStaticField',
            targetClass = Public,
            type = Open.Type.GET_STATIC
    )
    private static String publicPrivateStaticField() {
        throw new RuntimeException()
    }

    @Open(
            name = 'privateStaticField',
            targetClass = Public,
            type = Open.Type.SET_STATIC
    )
    private static void publicPrivateStaticField(String value) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateStaticField() {
        publicPrivateStaticField("test")
        assertEquals("test", publicPrivateStaticField())
    }

    @Open(
            name = 'protectedStaticField',
            targetClass = Public,
            type = Open.Type.GET_STATIC
    )
    private static String publicProtectedStaticField() {
        throw new RuntimeException()
    }

    @Open(
            name = 'protectedStaticField',
            targetClass = Public,
            type = Open.Type.SET_STATIC
    )
    private static void publicProtectedStaticField(String value) {
        throw new RuntimeException()
    }

    @Test
    void testProtectedStaticField() {
        publicProtectedStaticField("test")
        assertEquals("test", publicProtectedStaticField())
    }

    @Open(
            name = 'packagePrivateStaticField',
            targetClass = Public,
            type = Open.Type.GET_STATIC
    )
    private static String publicPackagePrivateStaticField() {
        throw new RuntimeException()
    }

    @Open(
            name = 'packagePrivateStaticField',
            targetClass = Public,
            type = Open.Type.SET_STATIC
    )
    private static void publicPackagePrivateStaticField(String value) {
        throw new RuntimeException()
    }

    @Test
    void testPackagePrivateStaticField() {
        publicPackagePrivateStaticField("test")
        assertEquals("test", publicPackagePrivateStaticField())
    }

    @Open(
            name = 'privateFinalStaticField',
            targetClass = Public,
            type = Open.Type.GET_STATIC
    )
    private static String publicPrivateFinalStaticField() {
        throw new RuntimeException()
    }

    @Test
    void testPrivateFinalStaticField() {
        assertEquals("privateFinalStaticField", publicPrivateFinalStaticField())
    }

    @Open(
            name = '<init>',
            targetName = 'dev.lukebemish.opensesame.test.target.Public$Private',
            type = Open.Type.CONSTRUCT
    )
    private static Object privatePrivateConstructor() {
        throw new RuntimeException()
    }

    @Open(
            name = 'privateInstance',
            targetName = 'dev.lukebemish.opensesame.test.target.Public$Private',
            type = Open.Type.VIRTUAL
    )
    private static String privatePrivateInstance(Object instance) {
        throw new RuntimeException()
    }

    @Test
    void testPrivateClass() {
        Object instance = privatePrivateConstructor()
        assertEquals("Private", instance.toString())
        assertEquals("privateInstance", privatePrivateInstance(instance))
    }

    @Open(
            name = '<init>',
            targetName = 'dev.lukebemish.opensesame.test.target.Public$PrivateCtor',
            type = Open.Type.CONSTRUCT
    )
    private static Public.PrivateCtor privateConstructor() {
        throw new RuntimeException()
    }

    @Test
    void testPrivateConstructor() {
        Public.PrivateCtor instance = privateConstructor()
        assertEquals("PrivateCtor", instance.toString())
    }

    @Open(
            name = '<init>',
            targetName = 'dev.lukebemish.opensesame.test.target.PackagePrivate',
            type = Open.Type.CONSTRUCT
    )
    private static Object packagePrivateConstructor() {
        throw new RuntimeException()
    }

    @Open(
            name = 'privateInstance',
            targetName = 'dev.lukebemish.opensesame.test.target.PackagePrivate',
            type = Open.Type.VIRTUAL
    )
    private static String packagePrivateInstance(Object instance) {
        throw new RuntimeException()
    }

    @Test
    void testPackagePrivateClass() {
        Object instance = packagePrivateConstructor()
        assertEquals("PackagePrivate", instance.toString())
        assertEquals("privateInstance", packagePrivateInstance(instance))
    }

    @Open(
            name = 'voidReturn',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static void publicVoidReturn() {
        throw new RuntimeException()
    }

    @Test
    void testVoidReturn() {
        int count = Public.voidReturnCounter
        publicVoidReturn()
        assertEquals(Public.voidReturnCounter, count + 1)
    }
    
    @Open(
            name = 'primitiveReturn',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static int publicPrimitiveReturn() {
        throw new RuntimeException()
    }
    
    @Test
    void testPrimitiveReturn() {
        assertEquals(publicPrimitiveReturn(), 5)
    }
    
    @Open(
            name = 'privateReturn',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static @Coerce(targetName = 'dev.lukebemish.opensesame.test.target.Public$Private') Object publicPrivateReturn() {
        throw new RuntimeException()
    }
    
    @Test
    void testPrivateReturn() {
        assertEquals("Private", publicPrivateReturn().toString())
    }
    
    @Open(
            name = 'arrayReturn',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String[] publicArrayReturn() {
        throw new RuntimeException()
    }
    
    @Test
    void testArrayReturn() {
        assertArrayEquals(new String[] {"a", "b"}, publicArrayReturn())
    }
    
    @Open(
            name = 'primitiveArgument',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String publicPrimitiveArgument(int value) {
        throw new RuntimeException()
    }
    
    @Test
    void testPrimitiveArgument() {
        assertEquals('5', publicPrimitiveArgument(5))
    }
    
    @Open(
            name = 'privateArgument',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String publicPrivateArgument(@Coerce(targetName = 'dev.lukebemish.opensesame.test.target.Public$Private') Object value) {
        throw new RuntimeException()
    }
    
    @Test
    void testPrivateArgument() {
        assertEquals("Private1", publicPrivateArgument(privatePrivateConstructor()))
    }
    
    @Open(
            name = 'arrayArgument',
            targetClass = Public,
            type = Open.Type.STATIC
    )
    private static String publicArrayArgument(String[] value) {
        throw new RuntimeException()
    }
    
    @Test
    void testArrayArgument() {
        assertEquals("a", publicArrayArgument(new String[]{"a", "b"}))
    }

    @Open(
            name = 'hiddenByModules',
            targetName = 'dev.lukebemish.opensesame.test.target.hidden.Hidden',
            type = Open.Type.STATIC
    )
    private static String hiddenByModules() {
        throw new RuntimeException()
    }

    @Open(
            name = 'hiddenByModulesPrivate',
            targetName = 'dev.lukebemish.opensesame.test.target.hidden.Hidden',
            type = Open.Type.STATIC
    )
    private static String hiddenByModulesPrivate() {
        throw new RuntimeException()
    }

    @Test
    void testModuleBreaking() {
        assertEquals('hiddenByModules', hiddenByModules())
        assertEquals('hiddenByModulesPrivate', hiddenByModulesPrivate())
        assertThrows(IllegalAccessException, {
            Class<?> hidden = Class.forName("dev.lukebemish.opensesame.test.target.hidden.Hidden");
            hidden.getDeclaredMethod("hiddenByModules").invoke(null);
        })
    }
}
