package dev.lukebemish.opensesame.plugin;

import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.inject.Inject;
import java.util.Locale;

public abstract class OpenSesameExtension implements ExtensionAware {
    private final Project project;

    @Inject
    public OpenSesameExtension(Project project) {
        this.project = project;
    }

    public void apply(SourceSet sourceSet, SourceDirectorySet sourceDirectorySet, TaskProvider<? extends AbstractCompile> compileTaskProvider) {
        var unprocessed = project.getLayout().getBuildDirectory().dir("openSesame/unprocessed/"+compileTaskProvider.getName());
        compileTaskProvider.configure(compileTask ->
                compileTask.getDestinationDirectory().set(unprocessed)
        );
        var capitalized = compileTaskProvider.getName().substring(0, 1).toUpperCase(Locale.ROOT) + compileTaskProvider.getName().substring(1);
        var openSesameTask = project.getTasks().register("openSesame" + capitalized, OpenSesameTask.class, task -> {
            task.getInputClasses().set(compileTaskProvider.get().getDestinationDirectory());
            task.getOutputClasses().set(sourceDirectorySet.getClassesDirectory());
        });
        sourceDirectorySet.compiledBy(openSesameTask, OpenSesameTask::getOutputClasses);
        project.getTasks().named(sourceSet.getClassesTaskName()).configure(task -> task.dependsOn(openSesameTask));
    }

    public void apply(SourceSet sourceSet) {
        apply(sourceSet, sourceSet.getJava(), project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class));
    }
}
