package dev.lukebemish.opensesame.runtime.fabric;

import com.google.auto.service.AutoService;
import dev.lukebemish.opensesame.runtime.RuntimeRemapper;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

@AutoService(RuntimeRemapper.class)
public class FabricRuntimeRemapper implements RuntimeRemapper {
    private static final String TARGET_NAMESPACE = "intermediary";
    @Override
    public @Nullable String remapMethodName(Class<?> parent, String name, Class<?>[] args, Class<?> returnType) {
        try {
            var originalClassName = FabricLoader.getInstance().getMappingResolver().unmapClassName(TARGET_NAMESPACE, parent.getName());
            StringBuilder methodDesc = new StringBuilder("(");
            for (var arg : args) {
                methodDesc.append(classToDesc(arg));
            }
            methodDesc.append(")");
            methodDesc.append(classToDesc(returnType));
            var newName = FabricLoader.getInstance().getMappingResolver().mapMethodName(TARGET_NAMESPACE, originalClassName, name, methodDesc.toString());
            if (!newName.equals(name))
                return newName;
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public @Nullable String remapFieldName(Class<?> parent, String name, Class<?> type) {
        try {
            var originalClassName = FabricLoader.getInstance().getMappingResolver().unmapClassName(TARGET_NAMESPACE, parent.getName());
            var fieldDesc = classToDesc(type);
            var newName = FabricLoader.getInstance().getMappingResolver().mapFieldName(TARGET_NAMESPACE, originalClassName, name, fieldDesc);
            if (!newName.equals(name))
                return newName;
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public @Nullable String remapClassName(String className) {
        try {
            var newName = FabricLoader.getInstance().getMappingResolver().mapClassName(TARGET_NAMESPACE, className);
            if (!newName.equals(className))
                return newName;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String classToDesc(Class<?> type) {
        if (type.isArray()) {
            return "[" + classToDesc(type.getComponentType());
        } else if (type.isPrimitive()) {
            return type.getName();
        } else {
            try {
                var name = FabricLoader.getInstance().getMappingResolver().unmapClassName(TARGET_NAMESPACE, type.getName());
                return "L"+name.replace('.', '/')+";";
            } catch (Exception e) {
                return "L"+type.getName().replace('.', '/')+";";
            }
        }
    }
}
