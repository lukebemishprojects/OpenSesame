package dev.lukebemish.opensesame.compile.javac;

import com.google.auto.service.AutoService;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.*;
import dev.lukebemish.opensesame.annotations.Coerce;
import dev.lukebemish.opensesame.annotations.Open;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;

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
            public void started(TaskEvent e) {
                if (e.getKind() != TaskEvent.Kind.GENERATE) {
                    return;
                }

                Elements elements = task.getElements();
                List<TypeMirror> types = new ArrayList<>();

                e.getCompilationUnit().accept(new TreeScanner<Void, Void>() {
                    JavacOpenProcessor processor = null;

                    @Override
                    public Void visitClass(ClassTree node, Void unused) {
                        processor = new JavacOpenProcessor(node, elements);
                        return super.visitClass(node, unused);
                    }

                    @Override
                    public Void visitMethod(MethodTree node, Void unused) {
                        if (processor == null) {
                            return super.visitMethod(node, unused);
                        }
                        boolean[] hasOpen = new boolean[1];
                        node.getModifiers().getAnnotations().forEach(a -> {
                            try {
                                AnnotationMirror annotation = (AnnotationMirror) Utils.JC_ANNOTATION_GET_ATTRIBUTE.invoke(a);
                                var binaryName = elements.getBinaryName((TypeElement) annotation.getAnnotationType().asElement());
                                if (binaryName.contentEquals(Open.class.getName())) {
                                    hasOpen[0] = true;
                                }
                            } catch (Throwable ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                        if (hasOpen[0]) {
                            throw new RuntimeException(processor.parameters(node, Coerce.class).toString());
                        }
                        return super.visitMethod(node, unused);
                    }
                }, null);
            }
        });
    }
}
