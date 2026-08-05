package dev.lukebemish.opensesame.compile.javac;

import com.google.auto.service.AutoService;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import dev.lukebemish.javacpostprocessor.PostProcessor;
import dev.lukebemish.opensesame.compile.asm.VisitingProcessor;
import org.objectweb.asm.ClassVisitor;

import javax.tools.JavaFileManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@AutoService(PostProcessor.class)
public class OpenSesameProcessor implements PostProcessor {

    @Override
    public void context(Context context) {
        this.visitedCheck.clear();
        context.task().addTaskListener(new TaskListener() {
            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() == TaskEvent.Kind.COMPILATION) {
                    for (var check : visitedCheck) {
                        try {
                            VisitingProcessor.sortMixinServices(check);
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    }
                }
            }
        });
    }

    @Override
    public String name() {
        return "dev.lukebemish.opensesame";
    }

    private final Set<Path> visitedCheck = new HashSet<>();

    @Override
    public ClassVisitor visit(ClassVisitor classVisitor, String binaryName, JavaFileManager javaFileManager, JavaFileManager.Location location) {
        VisitingProcessor.OutputPathResolver resolver = s -> {
            var fileForOutput = javaFileManager.getFileForOutput(
                    location,
                    "",
                    s,
                    null
            );
            return Paths.get(fileForOutput.toUri());
        };
        var processor = VisitingProcessor.makeProcessor(classVisitor, VisitingProcessor.ANNOTATIONS, resolver, null);
        
        try {
            var path = VisitingProcessor.getMixinServiceLocation(resolver);
            visitedCheck.add(path.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return processor;
    }
}
