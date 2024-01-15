package dev.lukebemish.opensesame.runtime.fabric;

import com.google.auto.service.AutoService;
import dev.lukebemish.opensesame.runtime.RuntimeRemapper;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodType;

@AutoService(RuntimeRemapper.class)
public class FabricRuntimeRemapper implements RuntimeRemapper {
    private static final String SOURCE_NAMESPACE = "intermediary";
    @Override
    public @Nullable String remapMethodName(Class<?> parent, String name, Class<?>[] args, Class<?> returnType) {
        try {
            var originalClassName = FabricLoader.getInstance().getMappingResolver().unmapClassName(SOURCE_NAMESPACE, parent.getName());
            var methodDesc = MethodType.methodType(returnType, args).descriptorString();
            var newName = FabricLoader.getInstance().getMappingResolver().mapMethodName(SOURCE_NAMESPACE, originalClassName, name, methodDesc);
            if (!newName.equals(name))
                return newName;
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public @Nullable String remapFieldName(Class<?> parent, String name, Class<?> type) {
        try {
            var originalClassName = FabricLoader.getInstance().getMappingResolver().unmapClassName(SOURCE_NAMESPACE, parent.getName());
            var fieldDesc = classToDesc(type);
            var newName = FabricLoader.getInstance().getMappingResolver().mapFieldName(SOURCE_NAMESPACE, originalClassName, name, fieldDesc);
            if (!newName.equals(name))
                return newName;
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public @Nullable String remapClassName(String className) {
        try {
            var newName = FabricLoader.getInstance().getMappingResolver().mapClassName(SOURCE_NAMESPACE, className);
            if (!newName.equals(className))
                return newName;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String classToDesc(Class<?> type) {
        return type.descriptorString();
    }
}
