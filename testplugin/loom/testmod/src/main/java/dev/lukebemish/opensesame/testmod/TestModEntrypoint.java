package dev.lukebemish.opensesame.testmod;

import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Field;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import dev.lukebemish.opensesame.mixin.annotations.UnFinal;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Climate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestModEntrypoint implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger("OpenSesame - Test Mod");

    @Extend(
            targetName = "net.minecraft.world.level.biome.Climate$RTree",
            unsafe = true
    )
    @UnFinal
    public interface Extension<T> {
        @Constructor
        static <A> Extension<A> constructor(@Field(name = "fallback") @Field.Final A fallback, @Coerce(targetName = "net.minecraft.world.level.biome.Climate$RTree$Node") Object node) {
            throw new UnsupportedOperationException("Constructor not replaced");
        }

        @Field(name = "fallback")
        T getFallback();

        @Overrides(name = "search")
        default T searchOverride(Climate.TargetPoint targetPoint, @Coerce(targetName = "net.minecraft.world.level.biome.Climate$DistanceMetric") Object distanceMetric) {
            return getFallback();
        }
    }

    @Extend(
            targetName = "net.minecraft.world.level.biome.Climate$DistanceMetric",
            unsafe = true
    )
    public interface DistanceMetric {
        @Constructor
        static DistanceMetric constructor() {
            throw new UnsupportedOperationException("Constructor not replaced");
        }

        @Overrides(name = "distance")
        default long distanceImpl(@Coerce(targetName = "net.minecraft.world.level.biome.Climate$RTree$Node") Object node, long[] ls) {
            return 0;
        }
    }

    @Open(
            targetName = "net.minecraft.world.level.biome.Climate$RTree$Leaf",
            type = Open.Type.CONSTRUCT
    )
    static <T> Object makeLeaf(Climate.ParameterPoint parameterPoint, T object) {
        throw new UnsupportedOperationException("Method not replaced");
    }

    @Open(
            name = "search",
            targetName = "net.minecraft.world.level.biome.Climate$RTree",
            type = Open.Type.VIRTUAL
    )
    static <T> T search(
            @Coerce(targetName = "net.minecraft.world.level.biome.Climate$RTree$Leaf") Object tree,
            Climate.TargetPoint targetPoint,
            @Coerce(targetName = "net.minecraft.world.level.biome.Climate$DistanceMetric") Object distanceMetric
    ) {
        throw new UnsupportedOperationException("Method not replaced");
    }

    @Open(
            name = "namespace",
            targetClass = ResourceLocation.class,
            type = Open.Type.SET_INSTANCE
    )
    @UnFinal
    static void setNamespace(ResourceLocation resourceLocation, String namespace) {
        throw new UnsupportedOperationException("Method not replaced");
    }

    @Override
    public void onInitialize() {
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
        var leaf = makeLeaf(parameterPoint, value);
        var tree = Extension.constructor(fallback, leaf);
        var distanceMetric = DistanceMetric.constructor();
        if (!fallback.equals(search(tree, targetPoint, distanceMetric))) {
            throw new AssertionError(String.format("Expected '%s', got '%s'", fallback, search(tree, targetPoint, distanceMetric)));
        } else {
            LOGGER.info("Successfully overrode Climate$RTree#search");
        }

        var resourceLocation = new ResourceLocation("minecraft", "test");
        setNamespace(resourceLocation, "test");
        if (!resourceLocation.getNamespace().equals("test")) {
            throw new AssertionError(String.format("Expected '%s', got '%s'", "test", resourceLocation.getNamespace()));
        } else {
            LOGGER.info("Successfully made ResourceLocation.namespace mutable");
        }
    }
}
