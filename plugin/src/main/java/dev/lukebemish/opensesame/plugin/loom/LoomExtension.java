package dev.lukebemish.opensesame.plugin.loom;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.compile.JavaCompile;

public abstract class LoomExtension {
    public abstract Property<String> getTargetNamespace();
    public abstract Property<String> getSourceNamespace();

    public LoomExtension() {
        getSourceNamespace().convention("named");
        getTargetNamespace().convention("intermediary");
    }

    private void checkForLoom() {
        try {
            Class.forName("net.fabricmc.loom.api.LoomGradleExtensionAPI");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("fabric-loom is not present, but expected by opensesame.loom extension");
        }
    }

    public void targetNamespace(String namespace) {
        getTargetNamespace().set(namespace);
    }

    public void sourceNamespace(String namespace) {
        getSourceNamespace().set(namespace);
    }

    public void apply(JavaCompile compileTask) {
        // TODO: We'll want to add a mapping thing instead
        checkForLoom();

        compileTask.getOptions().getCompilerArgs().add("-Dopensesame.remap.source=" + getSourceNamespace().get());
        compileTask.getOptions().getCompilerArgs().add("-Dopensesame.remap.target=" + getTargetNamespace().get());
        compileTask.getOptions().getCompilerArgs().add("-Dopensesame.remap.mappings=" + LoomUtils.getMappingsFile(compileTask.getProject()));
    }
}
