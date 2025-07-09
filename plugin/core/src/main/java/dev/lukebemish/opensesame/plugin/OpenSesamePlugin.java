package dev.lukebemish.opensesame.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class OpenSesamePlugin implements Plugin<Project> {
    static final String DEPENDENCY_CONFIGURATION_NAME = "_openSesameJavacPlugin";
    
    @Override
    public void apply(Project target) {
        target.getExtensions().create("opensesame", OpenSesameExtension.class);
        var dependencyConfiguration = target.getConfigurations().dependencyScope(DEPENDENCY_CONFIGURATION_NAME);
        var implVersion = OpenSesamePlugin.class.getPackage().getImplementationVersion();
        if (implVersion == null) {
            implVersion = "unspecified";
        }
        target.getDependencies().add(
                dependencyConfiguration.getName(),
                "dev.lukebemish.opensesame:opensesame-javac:"+implVersion
        );
    }
}
