package dev.lukebemish.opensesame.testmod;

import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Field;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import dev.lukebemish.opensesame.annotations.mixin.Expose;
import dev.lukebemish.opensesame.annotations.mixin.UnFinal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Climate;

// These are here to avoid issues with classloading mixin classes
public class TestModOpenedClasses {
    @Extend(
            targetName = "net.minecraft.world.level.biome.Climate$RTree",
            unsafe = false
    )
    @Expose
    @UnFinal
    public interface Extension<T> {
        @Constructor
        @Expose
        static <A> Extension<A> constructor(@Field(value = "fallback") @Field.Final A fallback, @Coerce(targetName = "net.minecraft.world.level.biome.Climate$RTree$Node") Object node) {
            throw new UnsupportedOperationException("Constructor not replaced");
        }

        @Field("fallback")
        T getFallback();

        @Overrides("search")
        default T searchOverride(Climate.TargetPoint targetPoint, @Coerce(targetName = "net.minecraft.world.level.biome.Climate$DistanceMetric") Object distanceMetric) {
            return getFallback();
        }
    }

    @Extend(
            targetName = "net.minecraft.world.level.biome.Climate$DistanceMetric",
            unsafe = false
    )
    @Expose
    public interface DistanceMetric {
        @Constructor
        static DistanceMetric constructor() {
            throw new UnsupportedOperationException("Constructor not replaced");
        }

        @Overrides(value = "distance")
        default long distanceImpl(@Coerce(targetName = "net.minecraft.world.level.biome.Climate$RTree$Node") Object node, long[] ls) {
            return 0;
        }
    }

    @Open(
            targetName = "net.minecraft.world.level.biome.Climate$RTree$Leaf",
            type = Open.Type.CONSTRUCT
    )
    public static <T> Object makeLeaf(Climate.ParameterPoint parameterPoint, T object) {
        throw new UnsupportedOperationException("Method not replaced");
    }

    @Open(
            name = "search",
            targetName = "net.minecraft.world.level.biome.Climate$RTree",
            type = Open.Type.VIRTUAL
    )
    public static <T> T search(
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
    public static void setNamespace(ResourceLocation resourceLocation, String namespace) {
        throw new UnsupportedOperationException("Method not replaced");
    }

    @Extend(
            targetClass = TestRecord.class,
            unsafe = false
    )
    @UnFinal
    public interface RecordExtension {
        @Constructor
        static RecordExtension constructor(@Field(value = "field") @Field.Final String field, int a, int b) {
            throw new UnsupportedOperationException("Constructor not replaced");
        }

        @Field("field")
        String field();
    }

    @Extend(
            targetClass = TestSealed.class,
            unsafe = false
    )
    @UnFinal
    public interface SealedClassExtension {
        @Constructor
        static SealedClassExtension constructor() {
            throw new UnsupportedOperationException("Constructor not replaced");
        }
    }
}
