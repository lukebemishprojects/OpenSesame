package dev.lukebemish.opensesame.test.metafactory.Extend;

import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Field;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestDynamicProxy {
    @Extend(
            targetName = "java.lang.invoke.MethodHandle$PolymorphicSignature",
            unsafe = true
    )
    public interface PackagePrivateInterfaceExtension {
        @Constructor
        static PackagePrivateInterfaceExtension constructor() {
            throw new AssertionError("Constructor not transformed");
        }
    }

    @Extend(
            targetName = "jdk.internal.access.SharedSecrets",
            unsafe = true
    )
    public interface ModuleDisallowedClassExtension {
        @Constructor
        static ModuleDisallowedClassExtension constructor() {
            throw new AssertionError("Constructor not transformed");
        }
    }

    @Extend(
            targetName = "jdk.internal.loader.URLClassPath$Loader",
            unsafe = true
    )
    public interface PrivateClassExtension {
        @Constructor
        static PrivateClassExtension constructor(@Field("field1") String field1, @Field("field2") int field2, URL url) {
            throw new AssertionError("Constructor not transformed");
        }
        
        @Field("field1")
        String getField1();
        
        @Field("field2")
        int getField2();
    }

    @Test
    void testImplementPackagePrivateInterface() throws ClassNotFoundException {
        var clazz = Class.forName("java.lang.invoke.MethodHandle$PolymorphicSignature");
        var instance = PackagePrivateInterfaceExtension.constructor();
        assertTrue(clazz.isAssignableFrom(instance.getClass()));
        // Check that it is indeed in a dynamic module
        assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
    }

    @Test
    void testExtendModuleDisallowedClass() throws ClassNotFoundException {
        var clazz = Class.forName("jdk.internal.access.SharedSecrets");
        var instance = ModuleDisallowedClassExtension.constructor();
        assertTrue(clazz.isAssignableFrom(instance.getClass()));
        // Check that it is indeed in a dynamic module
        assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
    }

    @Test
    void testExtendPrivateClass() throws ClassNotFoundException {
        var clazz = Class.forName("jdk.internal.loader.URLClassPath$Loader");
        var instance = PrivateClassExtension.constructor("string", 2, null);
        assertTrue(clazz.isAssignableFrom(instance.getClass()));
        // Check that it is indeed in a dynamic module
        assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
        assertEquals("string", instance.getField1());
        assertEquals(2, instance.getField2());
    }
}
