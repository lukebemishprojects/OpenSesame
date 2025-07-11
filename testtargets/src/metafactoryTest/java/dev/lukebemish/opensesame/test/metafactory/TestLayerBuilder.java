package dev.lukebemish.opensesame.test.metafactory;

import dev.lukebemish.opensesame.test.framework.LayerBuilder;
import dev.lukebemish.opensesame.test.framework.LayerTest;

public class TestLayerBuilder {
    @LayerTest
    public LayerBuilder layerBuilder() {
        return LayerBuilder.create().withModule("test.test", module ->
                module.test("test.test.TestSomething", """
                        @Test
                        void alwaysPass() {}
                        """)
                );
    }
}
