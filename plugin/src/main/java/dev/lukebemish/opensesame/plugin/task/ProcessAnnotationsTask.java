package dev.lukebemish.opensesame.plugin.task;

import dev.lukebemish.opensesame.compile.asm.VisitingOpenProcessor;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Path;

public abstract class ProcessAnnotationsTask extends DefaultTask {
    @InputDirectory
    public abstract DirectoryProperty getInputClassesDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputClassesDir();

    @TaskAction
    public void process() {
        try {
            Path inPath = getInputClassesDir().get().getAsFile().toPath();
            Path outPath = getOutputClassesDir().get().getAsFile().toPath();
            VisitingOpenProcessor.process(inPath, outPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
