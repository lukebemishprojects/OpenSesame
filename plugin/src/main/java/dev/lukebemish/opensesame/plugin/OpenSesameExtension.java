package dev.lukebemish.opensesame.plugin;

import dev.lukebemish.opensesame.plugin.task.ProcessAnnotationsTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.inject.Inject;

public abstract class OpenSesameExtension {
    private final Project project;

    @Inject
    public OpenSesameExtension(Project project) {
        this.project = project;
    }

    public void apply(SourceSet sourceSet) {
        var unprocessedClasses = project.getLayout().getBuildDirectory().dir("opensesame/raw/" + sourceSet.getName());
        var compileJava = project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class, task -> {
            task.getDestinationDirectory().set(unprocessedClasses);
        });
        var processOpenSesame = project.getTasks().register(sourceSet.getTaskName("process", "openSesame"), ProcessAnnotationsTask.class, task -> {
            task.dependsOn(compileJava);
            task.getInputClassesDir().set(unprocessedClasses);
        });
        sourceSet.getJava().compiledBy(processOpenSesame, ProcessAnnotationsTask::getOutputClassesDir);
        project.getTasks().named(sourceSet.getClassesTaskName(), task -> {
            task.dependsOn(processOpenSesame);
        });
    }
}
