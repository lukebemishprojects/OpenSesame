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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void apply() {
        LoomGradleExtensionAPI loomExtension = (LoomGradleExtensionAPI) project.getExtensions().getByName("loom");
        loomExtension.addRemapperExtension((Class) OpeningRemapperExtension.class, RemapperParameters.None.class, i -> {});
        loomExtension.getKnownIndyBsms().add("dev/lukebemish/opensesame/runtime/OpeningMetafactory");
    }
}
