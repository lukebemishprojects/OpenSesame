package dev.lukebemish.opensesame.test.metafactory.Extend;

import dev.lukebemish.opensesame.test.metafactory.OpenSesameLayerConfiguration;
import dev.lukebemish.testingutils.framework.modulelayer.LayerBuilder;
import dev.lukebemish.testingutils.framework.modulelayer.LayerConfiguration;
import dev.lukebemish.testingutils.framework.modulelayer.LayerTest;

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
                            
                                @Override
                                default void testImplemented() {
                                    assertNull(hasPackagePrivateType());
                                }
                            }""")
                        .java("opensesame.target.MethodParameterPackagePrivateInInterface", """
                            public interface MethodParameterPackagePrivateInInterface extends opensesame.target.api.TestImplemented {
                                void hasPackagePrivateType(PackagePrivateInterface param);
                            
                                @Override
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

    @LayerTest
    @LayerConfiguration(
            imports = "java.util.stream.IntStream"
    )
    public LayerBuilder testBounceGenerationClasses() {
        return LayerBuilder.create()
                .withModule("opensesame.target", module -> module
                        .java("opensesame.target.api.TestImplemented", """
                            public interface TestImplemented {
                                void testImplemented();
                            }""")
                        .exports("opensesame.target.api")
                        .java("opensesame.target.PackagePrivateClass", """
                            class PackagePrivateClass {}
                            """)
                        .java("opensesame.target.PrivateConstructorClass", """
                            class PrivateConstructorClass {
                                private final String field1;
                            
                                private PrivateConstructorClass() {
                                    this.field1 = "default";
                                }
                            
                                private PrivateConstructorClass(String parameter) {
                                    this.field1 = parameter;
                                }
                            
                                @Override
                                public String toString() {
                                    return "PrivateConstructorClass[" + field1 + "]";
                                }
                            }
                            """)
                        .java("opensesame.target.MethodReturnsPackagePrivateInClass", """
                            import static org.junit.jupiter.api.Assertions.*;
                            
                            public class MethodReturnsPackagePrivateInClass implements opensesame.target.api.TestImplemented {
                                PackagePrivateClass hasPackagePrivateType() {
                                    return new PackagePrivateClass();
                                }

                                @Override
                                public void testImplemented() {
                                    assertNull(hasPackagePrivateType());
                                }
                            }""")
                        .java("opensesame.target.MethodParameterPackagePrivateInClass", """
                            import static org.junit.jupiter.api.Assertions.*;
                            
                            public class MethodParameterPackagePrivateInClass implements opensesame.target.api.TestImplemented {
                                void hasPackagePrivateType(PackagePrivateClass param) {
                                    fail();
                                }
                            
                                @Override
                                public void testImplemented() {
                                    hasPackagePrivateType(null);
                                }
                            }"""))
                .child().withModule("opensesame.test", module -> module
                        .requires("opensesame.target")
                        .test("opensesame.test.TestBounceGeneration", """
                            @Extend(
                                    targetName = "opensesame.target.PackagePrivateClass",
                                    unsafe = true
                            )
                            public interface ExtendsPackagePrivateClass {
                                @Constructor
                                static ExtendsPackagePrivateClass constructor() {
                                    throw new AssertionError("Constructor not transformed");
                                }
                            }
                            
                            @Extend(
                                    targetName = "opensesame.target.PrivateConstructorClass",
                                    unsafe = true
                            )
                            public interface ExtendsPrivateConstructorClass {
                                @Constructor
                                static ExtendsPrivateConstructorClass constructorNoFieldsNoSuperArgs() {
                                    throw new AssertionError("Constructor not transformed");
                                }
                            
                                @Constructor
                                static ExtendsPrivateConstructorClass constructorFieldsNoSuperArgs(@Field("field1") String field1, @Field("field2") int field2) {
                                    throw new AssertionError("Constructor not transformed");
                                }
                                @Constructor
                                static ExtendsPrivateConstructorClass constructorNoFieldsSuperArgs(String parameter) {
                                    throw new AssertionError("Constructor not transformed");
                                }
                            
                                @Constructor
                                static ExtendsPrivateConstructorClass constructorFieldsSuperArgs(@Field("field1") String field1, @Field("field2") int field2, String parameter) {
                                    throw new AssertionError("Constructor not transformed");
                                }
                            
                                @Field("field1")
                                String getField1();
                            
                                @Field("field2")
                                int getField2();
                            }
                            
                            @Extend(
                                    targetName = "opensesame.target.MethodReturnsPackagePrivateInClass",
                                    unsafe = true
                            )
                            public interface ExtendsMethodReturnsPackagePrivateInClass extends opensesame.target.api.TestImplemented {
                                @Constructor
                                static ExtendsMethodReturnsPackagePrivateInClass constructor() {
                                    throw new AssertionError("Constructor not transformed");
                                }
                            
                                @Overrides("hasPackagePrivateType")
                                default @Coerce(targetName = "opensesame.target.PackagePrivateClass") Object hasPackagePrivateTypeImpl() {
                                    return null;
                                }
                            }
                            
                            @Extend(
                                    targetName = "opensesame.target.MethodParameterPackagePrivateInClass",
                                    unsafe = true
                            )
                            public interface ExtendsMethodParameterPackagePrivateInClass extends opensesame.target.api.TestImplemented {
                                @Constructor
                                static ExtendsMethodParameterPackagePrivateInClass constructor() {
                                    throw new AssertionError("Constructor not transformed");
                                }
                            
                                @Overrides("hasPackagePrivateType")
                                default void hasPackagePrivateTypeImpl(@Coerce(targetName = "opensesame.target.PackagePrivateClass") Object param) {}
                            }
                            
                            @Test
                            void testBouncePackagePrivateClass() throws ClassNotFoundException {
                                var instance = ExtendsPackagePrivateClass.constructor();
                                var targetClass = Class.forName("opensesame.target.PackagePrivateClass");
                                assertInstanceOf(targetClass, instance);
                                assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
                                assertTrue(instance.getClass().getSuperclass()
                                    .getName().contains("$$dev$lukebemish$opensesame$$BounceInterface")
                                );
                            }
                            
                            @Test
                            void testBouncePrivateConstructorRequiresAllocateInstance() throws ClassNotFoundException {
                                var instances = new ExtendsPrivateConstructorClass[] {
                                    ExtendsPrivateConstructorClass.constructorFieldsNoSuperArgs("field1", 2),
                                    ExtendsPrivateConstructorClass.constructorFieldsSuperArgs("field1", 2, "parameter"),
                                    ExtendsPrivateConstructorClass.constructorNoFieldsNoSuperArgs(),
                                    ExtendsPrivateConstructorClass.constructorNoFieldsSuperArgs("parameter")
                                };
                                var parameters = new String[] {"default", "parameter", "default", "parameter"};
                                var field1s = new String[] {"field1", "field1", null, null};
                                var field2s = new int[] {2, 2, 0, 0};
                                var targetClass = Class.forName("opensesame.target.PrivateConstructorClass");
                                IntStream.range(0, 4).forEach(i -> {
                                    var instance = instances[i];
                                    assertInstanceOf(targetClass, instance);
                                    assertEquals(field1s[i], instance.getField1());
                                    assertEquals(field2s[i], instance.getField2());
                                    assertEquals("PrivateConstructorClass[" + parameters[i] + "]", instance.toString());
                                    assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
                                    assertTrue(instance.getClass().getSuperclass()
                                        .getName().contains("$$dev$lukebemish$opensesame$$BounceInterface")
                                    );
                                });
                            }
                            
                            @Test
                            void testBouncePackagePrivateReturn() throws ClassNotFoundException {
                                var instance = ExtendsMethodReturnsPackagePrivateInClass.constructor();
                                instance.testImplemented();
                                var targetClass = Class.forName("opensesame.target.MethodReturnsPackagePrivateInClass");
                                assertInstanceOf(targetClass, instance);
                                assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
                                assertTrue(instance.getClass().getSuperclass()
                                    .getName().contains("$$dev$lukebemish$opensesame$$BounceInterface")
                                );
                            }
                            
                            @Test
                            void testBouncePackagePrivateParameter() throws ClassNotFoundException {
                                var instance = ExtendsMethodParameterPackagePrivateInClass.constructor();
                                instance.testImplemented();
                                var targetClass = Class.forName("opensesame.target.MethodParameterPackagePrivateInClass");
                                assertInstanceOf(targetClass, instance);
                                assertTrue(instance.getClass().getPackage().getName().contains("jdk.proxy"));
                                assertTrue(instance.getClass().getSuperclass()
                                    .getName().contains("$$dev$lukebemish$opensesame$$BounceInterface")
                                );
                            }
                            """));
    }
}
