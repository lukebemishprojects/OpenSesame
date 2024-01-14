package dev.lukebemish.opensesame.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class OpenSesamePlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.getExtensions().create("opensesame", OpenSesameExtension.class);
    }
}
