package dev.lukebemish.opensesame.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class OpenSesamePlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        var openSesameExtension = target.getExtensions().create("opensesame", OpenSesameExtension.class);
        //((ExtensionAware) openSesameExtension).getExtensions().create("loom", LoomExtension.class);
    }
}
