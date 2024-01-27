package dev.lukebemish.opensesame.runtime.fabric;

import com.google.auto.service.AutoService;
import dev.lukebemish.opensesame.runtime.RuntimeRemapper;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

@AutoService(RuntimeRemapper.class)
public class FabricRuntimeRemapper implements RuntimeRemapper {
    private static final String SOURCE_NAMESPACE = "intermediary";
    @Override
    public @Nullable String remapMethodName(String parent, String name, String methodDesc) {
        try {
            var originalClassName = FabricLoader.getInstance().getMappingResolver().unmapClassName(SOURCE_NAMESPACE, parent);
            var newName = FabricLoader.getInstance().getMappingResolver().mapMethodName(SOURCE_NAMESPACE, originalClassName, name, methodDesc);
            if (!newName.equals(name))
                return newName;
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public @Nullable String remapFieldName(String parent, String name, String descriptor) {
        try {
            var originalClassName = FabricLoader.getInstance().getMappingResolver().unmapClassName(SOURCE_NAMESPACE, parent);
            var newName = FabricLoader.getInstance().getMappingResolver().mapFieldName(SOURCE_NAMESPACE, originalClassName, name, descriptor);
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
}
