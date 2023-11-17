package dev.lukebemish.opensesame.compile.javac;

import com.google.auto.service.AutoService;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.*;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.runtime.OpeningMetafactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@AutoService(Plugin.class)
public class OpenSesamePlugin implements Plugin {
    // TODO: look at LambdaToMethod.makeMetafactoryIndyCall
    // well, turns out that doesn't work because of how it handles indy an condyn - so instead, we'll generate a bouncer class with metafactory methos

    public static final String PLUGIN_NAME = "OpenSesame";

    private static final MethodHandle JC_ANNOTATION_GET_ATTRIBUTE;

    static {
        try {
            JC_ANNOTATION_GET_ATTRIBUTE = OpeningMetafactory.invoke(
                    MethodHandles.lookup(),
                    "attribute",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.code.Attribute$Compound"),
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCAnnotation")
                    ),
                    Class.forName("com.sun.tools.javac.tree.JCTree$JCAnnotation"),
                    Open.Type.GET_INSTANCE.ordinal()
            ).getTarget().asType(MethodType.methodType(Object.class, AnnotationTree.class));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

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

                e.getCompilationUnit().accept(new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitMethod(MethodTree node, Void unused) {
                        var annotations = node.getModifiers().getAnnotations().stream().filter(a -> {
                            throw new RuntimeException(a.getAnnotationType().toString());
                        });
                        node.getParameters().forEach(p -> {
                            p.getModifiers().getAnnotations().forEach(a -> { throw new RuntimeException(a.getAnnotationType().toString()); });
                        });
                        return super.visitMethod(node, unused);
                    }

                    @Override
                    public Void visitAnnotation(AnnotationTree node, Void unused) {
                        try {
                            if (true) throw new RuntimeException(JC_ANNOTATION_GET_ATTRIBUTE.invoke(node).toString());
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                        return super.visitAnnotation(node, unused);
                    }
                }, null);
            }
        });
    }

    private boolean shouldInstrument(AnnotationTree parameter) {
        return false;
    }
}
