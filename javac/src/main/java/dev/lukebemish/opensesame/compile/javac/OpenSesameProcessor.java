package dev.lukebemish.opensesame.compile.javac;

import com.google.auto.service.AutoService;
import com.sun.source.util.JavacTask;
import dev.lukebemish.javacpostprocessor.PostProcessor;
import dev.lukebemish.opensesame.compile.asm.VisitingProcessor;
import org.objectweb.asm.ClassVisitor;

import javax.tools.JavaFileManager;
import java.nio.file.Paths;
import java.util.Arrays;

@AutoService(PostProcessor.class)
public class OpenSesameProcessor implements PostProcessor {
    @Override
    public void init(JavacTask javacTask) {
        
    }

    @Override
    public String name() {
        return "dev.lukebemish.opensesame";
    }

    @Override
    public ClassVisitor visit(ClassVisitor classVisitor, String binaryName, JavaFileManager javaFileManager, JavaFileManager.Location location) {
        return VisitingProcessor.makeProcessor(classVisitor, VisitingProcessor.ANNOTATIONS, s -> {
            var parts = s.split("[/\\\\]");
            var relativeName = parts[parts.length - 1];
            var rest = Arrays.copyOfRange(parts, 0, parts.length - 1);
            var packageName = String.join(".", rest);
            var fileForOutput = javaFileManager.getFileForOutput(
                    location,
                    packageName,
                    relativeName,
                    null
            );
            return Paths.get(fileForOutput.toUri());
        }, null);
    }
}
