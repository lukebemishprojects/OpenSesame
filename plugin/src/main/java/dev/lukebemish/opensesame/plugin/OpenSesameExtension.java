package dev.lukebemish.opensesame.plugin;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;

public abstract class OpenSesameExtension {
    private final Project project;

    @Inject
    public OpenSesameExtension(Project project) {
        this.project = project;
    }

    public void apply(SourceSet sourceSet) {
        // TODO: implement stuff
    }
}
