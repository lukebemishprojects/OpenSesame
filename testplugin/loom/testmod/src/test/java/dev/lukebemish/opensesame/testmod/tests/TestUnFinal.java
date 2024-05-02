package dev.lukebemish.opensesame.testmod.tests;

import dev.lukebemish.opensesame.testmod.TestModOpenedClasses;
import dev.lukebemish.opensesame.testmod.TestRecord;
import dev.lukebemish.opensesame.testmod.TestSealed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestUnFinal {
    @SuppressWarnings("UnreachableCode")
    @Test
    void testUnFinalOverride() {
        String value = "Normal";
        String fallback = "Fallback";
        var parameterPoint = new Climate.ParameterPoint(
                Climate.Parameter.point(0),
                Climate.Parameter.point(0),
                Climate.Parameter.point(0),
                Climate.Parameter.point(0),
                Climate.Parameter.point(0),
                Climate.Parameter.point(0),
                0
        );
        var targetPoint = new Climate.TargetPoint(
                0, 0, 0, 0, 0, 0
        );
        var leaf = TestModOpenedClasses.makeLeaf(parameterPoint, value);
        var tree = TestModOpenedClasses.Extension.constructor(fallback, leaf);
        var distanceMetric = TestModOpenedClasses.DistanceMetric.constructor();
        if (!fallback.equals(TestModOpenedClasses.search(tree, targetPoint, distanceMetric))) {
            throw new AssertionError(String.format("Expected '%s', got '%s'", fallback, TestModOpenedClasses.search(tree, targetPoint, distanceMetric)));
        }
    }

    @SuppressWarnings("UnreachableCode")
    @Test
    void testUnFinalMutable() {
        var resourceLocation = new ResourceLocation("minecraft", "test");
        TestModOpenedClasses.setNamespace(resourceLocation, "test");
        if (!resourceLocation.getNamespace().equals("test")) {
            throw new AssertionError(String.format("Expected '%s', got '%s'", "test", resourceLocation.getNamespace()));
        }
    }

    @SuppressWarnings("UnreachableCode")
    @Test
    void testExtendRecord() {
        var instance = TestModOpenedClasses.RecordExtension.constructor("fieldValue", 1, 2);
        Assertions.assertInstanceOf(TestRecord.class, instance);
        Assertions.assertEquals("TestRecord[a=1, b=2]", instance.toString());
        Assertions.assertEquals("fieldValue", instance.field());
    }

    @SuppressWarnings("UnreachableCode")
    @Test
    void testExtendSealed() {
        var instance = TestModOpenedClasses.SealedClassExtension.constructor();
        Assertions.assertInstanceOf(TestSealed.class, instance);
    }
}
