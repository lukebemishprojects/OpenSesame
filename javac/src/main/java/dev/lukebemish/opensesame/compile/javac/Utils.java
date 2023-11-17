package dev.lukebemish.opensesame.compile.javac;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.runtime.OpeningMetafactory;

import javax.lang.model.element.TypeElement;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class Utils {
    public static final MethodHandle JC_ANNOTATION_GET_ATTRIBUTE;
    public static final MethodHandle JC_VARIABLE_GET_SYMBOL;
    public static final MethodHandle JC_CLASS_GET_SYMBOL;

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

            JC_VARIABLE_GET_SYMBOL = OpeningMetafactory.invoke(
                    MethodHandles.lookup(),
                    "sym",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.code.Symbol"),
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCIdent")
                    ),
                    Class.forName("com.sun.tools.javac.tree.JCTree$JCIdent"),
                    Open.Type.GET_INSTANCE.ordinal()
            ).getTarget().asType(MethodType.methodType(Object.class, IdentifierTree.class));

            JC_CLASS_GET_SYMBOL = OpeningMetafactory.invoke(
                    MethodHandles.lookup(),
                    "sym",
                    MethodType.methodType(
                            Class.forName("com.sun.tools.javac.code.Symbol$ClassSymbol"),
                            Class.forName("com.sun.tools.javac.tree.JCTree$JCClassDecl")
                    ),
                    Class.forName("com.sun.tools.javac.tree.JCTree$JCClassDecl"),
                    Open.Type.GET_INSTANCE.ordinal()
            ).getTarget().asType(MethodType.methodType(TypeElement.class, ClassTree.class));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
