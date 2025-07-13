package dev.lukebemish.opensesame.test.metafactory.Extend;

import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Field;
import dev.lukebemish.opensesame.test.metafactory.OpenSesameLayerConfiguration;
import dev.lukebemish.testingutils.framework.modulelayer.LayerBuilder;
import dev.lukebemish.testingutils.framework.modulelayer.LayerTest;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class TestDynamicProxy implements OpenSesameLayerConfiguration {
    @LayerTest
    public LayerBuilder testBounceGenerationInterfaces() {
        return LayerBuilder.create()
                .withModule("opensesame.target", module -> module
                        .java("opensesame.target.api.TestImplemented", """
                            public interface TestImplemented {
                                void testImplemented();
                            }""")
                        .exports("opensesame.target.api")
                        .java("opensesame.target.PackagePrivateInterface", """
                            interface PackagePrivateInterface {}
                            """)
                        .java("opensesame.target.MethodReturnsPackagePrivateInInterface", """
                            import static org.junit.jupiter.api.Assertions.*;
                            
                            public interface MethodReturnsPackagePrivateInInterface extends opensesame.target.api.TestImplemented {
                                PackagePrivateInterface hasPackagePrivateType();
                                default void testImplemented() {
                                    assertNull(hasPackagePrivateType());
                                }
                            }""")
                        .java("opensesame.target.MethodParameterPackagePrivateInInterface", """
                            public interface MethodParameterPackagePrivateInInterface extends opensesame.target.api.TestImplemented {
                                void hasPackagePrivateType(PackagePrivateInterface param);
                                default void testImplemented() {
                                    hasPackagePrivateType(null);
                                }
                            }"""))
                .child().withModule("opensesame.test", module -> module
                        .requires("opensesame.target")
                        .test("opensesame.test.TestBounceGeneration", """
                            @Extend(
                                    targetName = "opensesame.target.PackagePrivateInterface",
                                    unsafe = true
                            )
                            public interface ExtendsPackagePrivateInterface {
                                @Constructor
                                static ExtendsPackagePrivateInterface constructor() {
                                    throw new AssertionError("Constructor not transformed");
                                }
                            }
                            
                            @Extend(
                                    targetName = "opensesame.target.MethodReturnsPackagePrivateInInterface",
                                    unsafe = true
                            )
                            public interface ExtendsMethodReturnsPackagePrivateInInterface extends opensesame.target.api.TestImplemented {
                                @Constructor
                                static ExtendsMethodReturnsPackagePrivateInInterface constructor() {
                                    throw new AssertionError("Constructor not transformed");
                                }
                            
                                @Overrides("hasPackagePrivateType")
                                default @Coerce(targetName = "opensesame.target.PackagePrivateInterface") Object hasPackagePrivateTypeImpl() {
                                    return null;
                                }
                            }
                            
                            @Extend(
                                    targetName = "opensesame.target.MethodParameterPackagePrivateInInterface",
                                    unsafe = true
                            )
                            public interface ExtendsMethodParameterPackagePrivateInInterface extends opensesame.target.api.TestImplemented {
                                @Constructor
                                static ExtendsMethodParameterPackagePrivateInInterface constructor() {
                                    throw new AssertionError("Constructor not transformed");
                                }
                            
                                @Overrides("hasPackagePrivateType")
                                default void hasPackagePrivateTypeImpl(@Coerce(targetName = "opensesame.target.PackagePrivateInterface") Object param) {}
                            }
                            
                            @Test
                            void testBouncePackagePrivateInterface() throws ClassNotFoundException {
                                var instance = ExtendsPackagePrivateInterface.constructor();
                                var targetClass = Class.forName("opensesame.target.PackagePrivateInterface");
                                assertInstanceOf(targetClass, instance);
                                assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
                                assertTrue(java.util.Arrays.stream(instance.getClass().getInterfaces()).anyMatch(
                                        clazz -> clazz.getName().contains("$$dev$lukebemish$opensesame$$BounceInterface")
                                ));
                            }
                            
                            @Test
                            void testBouncePackagePrivateReturn() throws ClassNotFoundException {
                                var instance = ExtendsMethodReturnsPackagePrivateInInterface.constructor();
                                instance.testImplemented();
                                var targetClass = Class.forName("opensesame.target.MethodReturnsPackagePrivateInInterface");
                                assertInstanceOf(targetClass, instance);
                                assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
                                assertTrue(java.util.Arrays.stream(instance.getClass().getInterfaces()).anyMatch(
                                        clazz -> clazz.getName().contains("$$dev$lukebemish$opensesame$$BounceInterface")
                                ));
                            }
                            
                            @Test
                            void testBouncePackagePrivateParameter() throws ClassNotFoundException {
                                var instance = ExtendsMethodParameterPackagePrivateInInterface.constructor();
                                instance.testImplemented();
                                var targetClass = Class.forName("opensesame.target.MethodParameterPackagePrivateInInterface");
                                assertInstanceOf(targetClass, instance);
                                assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
                                assertTrue(java.util.Arrays.stream(instance.getClass().getInterfaces()).anyMatch(
                                        clazz -> clazz.getName().contains("$$dev$lukebemish$opensesame$$BounceInterface")
                                ));
                            }
                            """));
    }
    
    @Test
    void testAlwaysFails() {
        fail();
    }
    
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
