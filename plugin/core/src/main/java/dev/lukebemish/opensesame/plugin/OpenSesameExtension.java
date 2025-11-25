package dev.lukebemish.opensesame.plugin;

import dev.lukebemish.javacpostprocessor.plugin.PostProcessorExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Function;

public abstract class OpenSesameExtension implements ExtensionAware {
    private final Project project;

    @Inject
    public OpenSesameExtension(Project project) {
        this.project = project;
    }

    public TaskProvider<OpenSesameTask> apply(SourceSet sourceSet, SourceDirectorySet sourceDirectorySet, TaskProvider<? extends AbstractCompile> compileTaskProvider) {
        return apply(sourceSet, sourceDirectorySet, compileTaskProvider, AbstractCompile::getDestinationDirectory);
    }

    public <T extends Task> TaskProvider<OpenSesameTask> apply(SourceSet sourceSet, SourceDirectorySet sourceDirectorySet, TaskProvider<T> compileTaskProvider, Function<T, DirectoryProperty> mapping) {
        var unprocessed = project.getLayout().getBuildDirectory().dir("openSesame/unprocessed/"+compileTaskProvider.getName());
        compileTaskProvider.configure(compileTask ->
                mapping.apply(compileTask).set(unprocessed)
        );
        var capitalized = compileTaskProvider.getName().substring(0, 1).toUpperCase(Locale.ROOT) + compileTaskProvider.getName().substring(1);
        var openSesameTask = project.getTasks().register("openSesame" + capitalized, OpenSesameTask.class, task -> {
            task.getInputClasses().set(mapping.apply(compileTaskProvider.get()));
            task.getOutputClasses().set(sourceDirectorySet.getClassesDirectory());
        });
        sourceDirectorySet.compiledBy(openSesameTask, OpenSesameTask::getOutputClasses);
        ((ConfigurableFileCollection) sourceSet.getOutput().getClassesDirs()).builtBy(openSesameTask);
        project.getTasks().named(sourceSet.getClassesTaskName()).configure(task -> task.dependsOn(openSesameTask));
        
        // Fix classes secondary variant...
        project.getConfigurations().configureEach(c -> {
            c.getOutgoing().getVariants().configureEach(v -> {
                replaceArtifacts(openSesameTask, v.getArtifacts(), c.getName(), c.getArtifacts());
            });
            replaceArtifacts(openSesameTask, c.getArtifacts(), c.getName(), c.getArtifacts());
        });
        
        return openSesameTask;
    }

    private void replaceArtifacts(TaskProvider<OpenSesameTask> openSesameTask, PublishArtifactSet artifacts, String name, PublishArtifactSet delegate) {
        var path = openSesameTask.get().getOutputClasses().get().getAsFile().toPath().toAbsolutePath();
        var matching = artifacts.matching(a -> a.getFile().toPath().toAbsolutePath().equals(path));
        var removed = new ArrayList<>(matching);
        removed.forEach(artifacts::remove);
        for (var artifact : removed) {
            var created = project.getArtifacts().add(name, path.toFile(), a -> {
                a.builtBy(openSesameTask);
                a.setExtension(artifact.getExtension());
                a.setClassifier(artifact.getClassifier());
                a.setType(artifact.getType());
                a.setName(artifact.getName());
            });
            delegate.remove(created);
            artifacts.add(created);
        }
    }

    public TaskProvider<OpenSesameTask> apply(SourceSet sourceSet) {
        return apply(sourceSet, sourceSet.getJava(), project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class));
    }

    public void applyJavac(SourceSet sourceSet, TaskProvider<? extends AbstractCompile> compileTaskProvider) {
        project.getPluginManager().apply("dev.lukebemish.javac-post-processor");
        compileTaskProvider.configure(javaCompile -> {
            javaCompile.getExtensions().getByType(PostProcessorExtension.class).getPlugins().add("dev.lukebemish.opensesame");
        });
        project.getConfigurations().named(sourceSet.getAnnotationProcessorConfigurationName()).configure(config ->
                config.extendsFrom(project.getConfigurations().getByName(OpenSesamePlugin.DEPENDENCY_CONFIGURATION_NAME))
        );
    }

    public void applyJavac(SourceSet sourceSet) {
        applyJavac(sourceSet, project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class));
    }
}
