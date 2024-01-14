package dev.lukebemish.opensesame.plugin.loom;

import dev.lukebemish.opensesame.plugin.OpenSesameExtension;
import dev.lukebemish.opensesame.plugin.OpenSesamePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class OpenSesamePluginLoom implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.getPlugins().apply(OpenSesamePlugin.class);

        var openSesameExtension = (OpenSesameExtension) target.getExtensions().getByName("opensesame");
        openSesameExtension.getExtensions().create("loom", LoomExtension.class);
    }
}
