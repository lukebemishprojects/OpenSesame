package dev.lukebemish.opensesame.testmod;

import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Field;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import dev.lukebemish.opensesame.annotations.mixin.Expose;
import dev.lukebemish.opensesame.annotations.mixin.UnFinal;
import dev.lukebemish.opensesame.test.target.Final;
import dev.lukebemish.opensesame.test.target.Public;
import dev.lukebemish.opensesame.test.target.RecordClass;
import dev.lukebemish.opensesame.test.target.SealedClass;
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
            targetClass = RecordClass.class,
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
            targetClass = SealedClass.class,
            unsafe = false
    )
    @UnFinal
    public interface SealedClassExtension {
        @Constructor
        static SealedClassExtension constructor() {
            throw new UnsupportedOperationException("Constructor not replaced");
        }
    }

    @Extend(
            targetClass = Final.class,
            unsafe = false
    )
    @UnFinal
    public interface FinalExtension {
        @Constructor
        static FinalExtension constructor() {
            throw new AssertionError("Constructor not replaced");
        }
    }

    @Extend(
            targetClass = Public.class,
            unsafe = false
    )
    public interface PublicExtension {
        @Constructor
        static PublicExtension constructor() {
            throw new AssertionError("Constructor not replaced");
        }

        @Overrides(value = "finalMethod")
        @UnFinal
        default String finalMethodOverride() {
            return "not so final now!";
        }
    }

    @Open(
            name = "privateFinalInstanceField",
            targetClass = Public.class,
            type = Open.Type.SET_INSTANCE
    )
    @UnFinal
    public static void privateFinalInstanceField(Public it, String value) {
        throw new AssertionError("Method not replaced");
    }

    @Open(
            name = "privateFinalInstanceField",
            targetClass = Public.class,
            type = Open.Type.GET_INSTANCE
    )
    public static String privateFinalInstanceField(Public it) {
        throw new AssertionError("Method not replaced");
    }

    @Open(
            name = "privateFinalStaticField",
            targetClass = Public.class,
            type = Open.Type.SET_STATIC
    )
    @UnFinal
    public static void privateFinalStaticField(String value) {
        throw new AssertionError("Method not replaced");
    }

    @Open(
            name = "privateFinalStaticField",
            targetClass = Public.class,
            type = Open.Type.GET_STATIC
    )
    public static String privateFinalStaticField() {
        throw new AssertionError("Method not replaced");
    }
}
