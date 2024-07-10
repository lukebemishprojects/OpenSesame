package dev.lukebemish.opensesame.plugin;

import dev.lukebemish.opensesame.compile.asm.VisitingProcessor;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.LocalState;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.InputChanges;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@CacheableTask
public abstract class OpenSesameTask extends DefaultTask {
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    @IgnoreEmptyDirectories
    @SkipWhenEmpty
    public abstract DirectoryProperty getInputClasses();

    @OutputDirectory
    public abstract DirectoryProperty getOutputClasses();

    @LocalState
    public abstract RegularFileProperty getIncrementalClasses();

    @Inject
    public OpenSesameTask() {
        getIncrementalClasses().convention(getProject().getLayout().getBuildDirectory().file("openSesame/incrementalClasses/"+getName()));
    }

    @TaskAction
    protected void process(InputChanges changes) throws IOException {
        var outputDir = getOutputClasses().get().getAsFile().toPath();
        var inputDir = getInputClasses().get().getAsFile().toPath();
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        Set<String> toProcess = new LinkedHashSet<>();
        var incrementalClassesFile = getIncrementalClasses().get().getAsFile();
        if (incrementalClassesFile.exists()) {
            toProcess.addAll(Files.readAllLines(incrementalClassesFile.toPath()));
        }
        changes.getFileChanges(getInputClasses()).forEach(change -> {
            var path = change.getFile().toPath();
            var relativePath = inputDir.relativize(path).toString();
            var outputPath = outputDir.resolve(relativePath);
            if (change.getChangeType() == ChangeType.REMOVED) {
                toProcess.remove(relativePath);
                if (Files.exists(outputPath)) {
                    try {
                        Files.delete(outputPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                toProcess.add(relativePath);
            }
        });
        VisitingProcessor.cleanup(outputDir);
        List<String> processed = new ArrayList<>();
        for (var relativePath : toProcess) {
            Files.createDirectories(outputDir.resolve(relativePath).getParent());
            var inputPath = inputDir.resolve(relativePath);
            var outputPath = outputDir.resolve(relativePath);
            if (VisitingProcessor.processFile(inputPath, outputPath, outputDir)) {
                processed.add(relativePath);
            }
        }
        Files.write(incrementalClassesFile.toPath(), processed);
    }
}
