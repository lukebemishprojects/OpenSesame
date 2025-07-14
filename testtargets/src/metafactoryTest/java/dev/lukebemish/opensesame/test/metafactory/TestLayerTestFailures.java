package dev.lukebemish.opensesame.test.metafactory;

import dev.lukebemish.testingutils.framework.modulelayer.LayerBuilder;
import dev.lukebemish.testingutils.framework.modulelayer.LayerTest;

public class TestLayerTestFailures {
    @LayerTest
    public LayerBuilder testFailures() {
        return LayerBuilder.create()
                .withModule("test.target", module -> module
                        .java("test.target.TargetClass", """
                                public class TargetClass {
                                    public void targetMethod() {
                                        throw new AssertionError("Whoops!");
                                    }
                                }
                                """)
                        .exports("test.target"))
                .withModule("test.test", module -> module
                        .requires("test.target")
                        .test("test.test.TestStuff", """
                                @Test
                                void testTargetMethod() {
                                    new test.target.TargetClass().targetMethod();
                                }
                                
                                @Test
                                void testInlineFailure() {
                                    assertTrue(false);
                                }
                                """));
    }
}
