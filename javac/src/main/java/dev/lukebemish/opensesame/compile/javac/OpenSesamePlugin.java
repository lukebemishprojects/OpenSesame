package dev.lukebemish.opensesame.compile.javac;

import com.google.auto.service.AutoService;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import dev.lukebemish.opensesame.compile.asm.VisitingProcessor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

@AutoService(Plugin.class)
public class OpenSesamePlugin implements Plugin {
    public static final String PLUGIN_NAME = "OpenSesame";

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
        task.addTaskListener(new TaskListener() {
            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() != TaskEvent.Kind.GENERATE) {
                    return;
                }

                Elements elements = task.getElements();
                var element = e.getTypeElement();
                if (element == null) {
                    return;
                }
                try {
                    var taskCtx = Utils.getContext(task);
                    var fileManager = Utils.getFromContext(taskCtx, JavaFileManager.class);
                    var classWriter = Utils.getClassWriter(taskCtx);
                    JavaFileManager.Location fileLocation;
                    if (Utils.isMultiModuleMode(classWriter)) {
                        Element moduleElement = element;
                        while (!(moduleElement instanceof ModuleElement)) {
                            if (moduleElement instanceof PackageElement packageElement) {
                                moduleElement = packageElement.getEnclosingElement();
                            } else if (moduleElement instanceof TypeElement typeElement) {
                                moduleElement = typeElement.getEnclosingElement();
                            }
                        }
                        fileLocation = fileManager.getLocationForModule(CLASS_OUTPUT, ((ModuleElement) moduleElement).getQualifiedName().toString());
                    } else {
                        fileLocation = StandardLocation.CLASS_OUTPUT;
                    }
                    var outFile = fileManager.getJavaFileForOutput(
                            fileLocation,
                            elements.getBinaryName(element).toString(),
                            JavaFileObject.Kind.CLASS,
                            e.getSourceFile()
                    );
                    try (var is = outFile.openInputStream()) {
                        var reader = new ClassReader(is);
                        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                        reader.accept(new VisitingProcessor(writer, VisitingProcessor.ANNOTATIONS), 0);
                        try (var os = outFile.openOutputStream()) {
                            os.write(writer.toByteArray());
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
