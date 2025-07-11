package dev.lukebemish.opensesame.test.metafactory;

import dev.lukebemish.opensesame.test.framework.LayerBuilder;
import dev.lukebemish.opensesame.test.framework.LayerTest;

public class TestLayerBuilder {
    @LayerTest
    public LayerBuilder layerBuilder() {
        // TODO: Build all the tests I actually wanted to using this framework

        return LayerBuilder.create()
                .withModule("test.test", module -> module
                        .test("test.test.TestThatClassesAreProcessed", """
                            @Open(
                                    name = "staticMethod",
                                    targetClass = test.target.Public.class,
                                    type = Open.Type.STATIC
                            )
                            private static String withStaticMethod() {
                                throw new AssertionError("Not transformed");
                            }
                            
                            @Test
                            void testOpenInEnvironment() {
                                assertEquals("staticMethod", withStaticMethod());
                            }
                            """)
                        .requires("test.target"))
                .withModule("test.target", module -> module
                        .java("test.target.Public", """
                            public class Public {
                                private static String staticMethod() {
                                    return "staticMethod";
                                }
                            }""")
                        .opens("test.target")
                        .exports("test.target")
        );
    }
}
