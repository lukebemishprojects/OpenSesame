package dev.lukebemish.opensesame.natives;

import dev.lukebemish.opensesame.runtime.RuntimeRemapper;
import org.jetbrains.annotations.Nullable;

public class ModuleRequirementShim implements RuntimeRemapper {
    @Override
    public @Nullable String remapMethodName(String parent, String name, String methodDesc) {
        return null;
    }

    @Override
    public @Nullable String remapFieldName(String parent, String name, String descriptor) {
        return null;
    }

    @Override
    public @Nullable String remapClassName(String className) {
        return null;
    }
}
