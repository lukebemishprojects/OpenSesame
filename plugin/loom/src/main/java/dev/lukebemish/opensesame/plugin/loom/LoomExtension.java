package dev.lukebemish.opensesame.plugin.loom;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.api.remapping.RemapperParameters;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class LoomExtension {
    private final Project project;

    @Inject
    public LoomExtension(Project project) {
        this.project = project;
    }

    private void checkForLoom() {
        try {
            Class.forName("net.fabricmc.loom.api.LoomGradleExtensionAPI");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("fabric-loom is not present, but expected by opensesame.loom extension");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void apply() {
        checkForLoom();

        LoomGradleExtensionAPI loomExtension = (LoomGradleExtensionAPI) project.getExtensions().getByName("loom");
        loomExtension.addRemapperExtension((Class) OpeningRemapperExtension.class, RemapperParameters.None.class, i -> {});
    }
}
