package dev.lukebemish.opensesame.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ServiceLoader;

public final class OpeningMetafactory {
    private OpeningMetafactory() {}

    private static final RuntimeRemapper remapper = load(RuntimeRemapper.class);

    private static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz, clazz.getClassLoader())
                .findFirst()
                .orElse(null);
    }

    public static CallSite invokeStatic(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            if (remapper != null) {
                String remapMethodName = remapper.remapMethodName(holdingClass, targetMethodName, factoryType.parameterArray());
                if (remapMethodName != null) {
                    targetMethodName = remapMethodName;
                }
            }
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findStatic(holdingClass, targetMethodName, factoryType);
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static CallSite invokeInstance(MethodHandles.Lookup caller, String targetMethodName, MethodType factoryType, Class<?> holdingClass) {
        try {
            if (remapper != null) {
                String remapMethodName = remapper.remapMethodName(holdingClass, targetMethodName, factoryType.parameterArray());
                if (remapMethodName != null) {
                    targetMethodName = remapMethodName;
                }
            }
            var handle = MethodHandles.privateLookupIn(holdingClass, MethodHandles.lookup()).findVirtual(holdingClass, targetMethodName, factoryType.dropParameterTypes(0, 1));
            return new ConstantCallSite(handle);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
