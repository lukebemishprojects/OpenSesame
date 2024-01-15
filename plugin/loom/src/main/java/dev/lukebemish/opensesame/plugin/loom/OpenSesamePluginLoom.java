package dev.lukebemish.opensesame.plugin.loom;

import dev.lukebemish.opensesame.plugin.OpenSesamePlugin;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.api.remapping.RemapperParameters;
import net.fabricmc.loom.bootstrap.LoomGradlePluginBootstrap;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class OpenSesamePluginLoom implements Plugin<Project> {

    @Override@SuppressWarnings({"unchecked", "rawtypes"})
    public void apply(Project target) {
        target.getPlugins().apply(OpenSesamePlugin.class);
        target.getPlugins().apply(LoomGradlePluginBootstrap.class);

        LoomGradleExtensionAPI loomExtension = (LoomGradleExtensionAPI) target.getExtensions().getByName("loom");
        loomExtension.addRemapperExtension((Class) OpeningRemapperExtension.class, RemapperParameters.None.class, i -> {});
        loomExtension.getKnownIndyBsms().add("dev/lukebemish/opensesame/runtime/OpeningMetafactory");
    }
}
