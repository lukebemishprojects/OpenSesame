package dev.lukebemish.opensesame.compile.javac;

import com.sun.source.tree.*;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.compile.ConDynUtils;
import dev.lukebemish.opensesame.compile.OpenProcessor;
import dev.lukebemish.opensesame.compile.TypeProvider;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;

class JavacOpenProcessor implements OpenProcessor<Type, AnnotationTree, MethodTree> {
    private final ClassTree enclosingClass;
    private final Elements elements;

    JavacOpenProcessor(ClassTree enclosingClass, Elements elements) {
        this.enclosingClass = enclosingClass;
        this.elements = elements;
    }

    @Override
    public TypeProvider<Type, ?, ?> types() {
        return ASMTypeProvider.INSTANCE;
    }

    @Override
    public ConDynUtils<Type, ?, ?> conDynUtils() {
        return ASMTypeProvider.CON_DYN_UTILS;
    }

    @Override
    public Object typeProviderFromAnnotation(AnnotationTree annotation, MethodTree method, Class<?> annotationType) {
        // TODO implement
        return null;
    }

    @Override
    public @Nullable AnnotationTree annotation(MethodTree method, Class<?> type) {
        AnnotationTree[] found = new AnnotationTree[1];
        method.getModifiers().getAnnotations().forEach(a -> {
            try {
                AnnotationMirror annotation = (AnnotationMirror) Utils.JC_ANNOTATION_GET_ATTRIBUTE.invoke(a);
                var binaryName = elements.getBinaryName((TypeElement) annotation.getAnnotationType().asElement());
                if (binaryName.contentEquals(type.getName())) {
                    if (found[0] != null) {
                        throw new RuntimeException("Method " + method.getName() + " may have at most one annotation of type " + type.getSimpleName() + ", but had more than one");
                    }
                    found[0] = a;
                }
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
        });
        return found[0];
    }

    @Override
    public List<MethodParameter<Type, AnnotationTree>> parameters(MethodTree method, Class<?> type) {
        return method.getParameters().stream().map(variableTree -> {
            AnnotationTree[] found = new AnnotationTree[1];
            variableTree.getModifiers().getAnnotations().forEach(a -> {
                try {
                    AnnotationMirror annotation = (AnnotationMirror) Utils.JC_ANNOTATION_GET_ATTRIBUTE.invoke(a);
                    var binaryName = elements.getBinaryName((TypeElement) annotation.getAnnotationType().asElement());
                    if (binaryName.contentEquals(type.getName())) {
                        if (found[0] != null) {
                            throw new RuntimeException("Method " + method.getName() + " parameter " + variableTree.getName() + " may have at most one annotation of type " + type.getSimpleName() + ", but had more than one");
                        }
                        found[0] = a;
                    }
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            });
            try {
                String descriptor;
                if (variableTree.getType() instanceof IdentifierTree identifierTree) {
                    TypeElement paramType = (TypeElement) Utils.JC_VARIABLE_GET_SYMBOL.invoke(identifierTree);
                    descriptor = "L"+elements.getBinaryName(paramType).toString().replace('.','/')+";";
                } else {
                    PrimitiveTypeTree primitiveTypeTree = (PrimitiveTypeTree) variableTree.getType();
                    descriptor = switch (primitiveTypeTree.getPrimitiveTypeKind()) {
                        case BOOLEAN -> "Z";
                        case BYTE -> "B";
                        case SHORT -> "S";
                        case INT -> "I";
                        case LONG -> "J";
                        case CHAR -> "C";
                        case FLOAT -> "F";
                        case DOUBLE -> "D";
                        case VOID -> "V";
                        default -> throw new RuntimeException("Unknown primitive type " + primitiveTypeTree.getPrimitiveTypeKind());
                    };
                }
                return new MethodParameter<>(types().type(descriptor), found[0]);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    @Override
    public Open.Type type(AnnotationTree annotation) {
        // TODO implement
        return null;
    }

    @Override
    public String name(AnnotationTree annotation) {
        // TODO implement
        return null;
    }

    @Override
    public Type returnType(MethodTree method) {
        Tree outType = method.getReturnType();
        try {
            String descriptor;
            if (outType instanceof IdentifierTree identifierTree) {
                TypeElement paramType = (TypeElement) Utils.JC_VARIABLE_GET_SYMBOL.invoke(identifierTree);
                descriptor = "L"+elements.getBinaryName(paramType).toString().replace('.','/')+";";
            } else {
                PrimitiveTypeTree primitiveTypeTree = (PrimitiveTypeTree) outType;
                descriptor = switch (primitiveTypeTree.getPrimitiveTypeKind()) {
                    case BOOLEAN -> "Z";
                    case BYTE -> "B";
                    case SHORT -> "S";
                    case INT -> "I";
                    case LONG -> "J";
                    case CHAR -> "C";
                    case FLOAT -> "F";
                    case DOUBLE -> "D";
                    case VOID -> "V";
                    default -> throw new RuntimeException("Unknown primitive type " + primitiveTypeTree.getPrimitiveTypeKind());
                };
            }
            return types().type(descriptor);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isStatic(MethodTree method) {
        return method.getModifiers().getFlags().contains(Modifier.STATIC);
    }

    @Override
    public String methodName(MethodTree method) {
        return method.getName().toString();
    }

    @Override
    public Type declaringClass(MethodTree method) {
        try {
            String name = elements.getBinaryName((TypeElement) Utils.JC_CLASS_GET_SYMBOL.invoke(enclosingClass)).toString().replace('.','/');
            return types().type("L"+name+";");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
