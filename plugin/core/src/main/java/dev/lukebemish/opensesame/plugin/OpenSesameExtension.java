package dev.lukebemish.opensesame.plugin;

import dev.lukebemish.opensesame.compile.asm.VisitingOpenProcessor;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;

public abstract class OpenSesameExtension implements ExtensionAware {
    private final Project project;

    @Inject
    public OpenSesameExtension(Project project) {
        this.project = project;
    }

    public void apply(TaskProvider<? extends AbstractCompile> compileTaskProvider) {
        compileTaskProvider.configure(compileTask ->
                compileTask.doLast("processOpenSesame", task -> {
                    try {
                        Path classesPath = compileTask.getDestinationDirectory().get().getAsFile().toPath();
                        VisitingOpenProcessor.process(classesPath, classesPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    public void apply(SourceSet sourceSet) {
        apply(project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class));
    }
}
