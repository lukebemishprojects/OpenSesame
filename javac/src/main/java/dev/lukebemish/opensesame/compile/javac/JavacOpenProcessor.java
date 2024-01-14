package dev.lukebemish.opensesame.compile.javac;

import com.sun.source.tree.*;
import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.compile.ConDynUtils;
import dev.lukebemish.opensesame.compile.OpenProcessor;
import dev.lukebemish.opensesame.compile.TypeProvider;
import dev.lukebemish.opensesame.compile.asm.ASMTypeProvider;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.function.Function;

class JavacOpenProcessor implements OpenProcessor<Type, AnnotationTree, MethodTree> {
    private final Elements elements;
    public final Type declaringClassType;

    JavacOpenProcessor(ClassTree enclosingClass, Elements elements) {
        this.elements = elements;
        try {
            String name = elements.getBinaryName((TypeElement) Utils.JC_CLASS_GET_SYMBOL.invoke(enclosingClass)).toString().replace('.','/');
            this.declaringClassType = types().type("L"+name+";");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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
    public ConDynUtils.TypedDynamic<?, Type> typeProviderFromAnnotation(AnnotationTree annotation, MethodTree method, Class<?> annotationType) {
        ConDynUtils.TypedDynamic<?, Type> targetClassHandle = null;

        String targetName = null;
        Type targetClass = null;
        Type targetFunction = null;

        Tree targetNameTree = findAnnotationArgument(annotation, "targetName");
        if (targetNameTree instanceof LiteralTree literalTree) {
            targetName = (String) literalTree.getValue();
        } else if (targetNameTree != null) {
            throw new RuntimeException("Expected argument targetName to be a string in "+annotation);
        }

        Tree targetClassTree = findAnnotationArgument(annotation, "targetClass");
        if (targetClassTree instanceof MemberSelectTree memberSelectTree) {
            var target = memberSelectTree.getExpression();
            targetClass = types().type(typeFromTree(target));
        }

        Tree targetFunctionTree = findAnnotationArgument(annotation, "targetProvider");
        if (targetFunctionTree instanceof MemberSelectTree memberSelectTree) {
            var target = memberSelectTree.getExpression();
            if (target instanceof IdentifierTree identifierTree) {
                try {
                    TypeElement type = (TypeElement) Utils.JC_VARIABLE_GET_SYMBOL.invoke(identifierTree);
                    targetFunction = types().type("L"+elements.getBinaryName(type).toString().replace('.','/')+";");
                } catch (Throwable ignored) {}
            }
        }
        if (targetFunction == null && targetFunctionTree != null) {
            throw new RuntimeException("Expected argument targetProvider to be a string in "+annotation);
        }

        if (targetName == null && targetClass == null && targetFunction == null) {
            throw new RuntimeException(annotationType.getSimpleName()+" annotation must have exactly one of targetName, targetClass, or targetProvider");
        } else if (targetName != null && targetFunction == null) {
            targetClassHandle = conDynUtils().conDynFromName(targetName, Function.identity());
        }
        if (targetClass != null) {
            if (targetClassHandle != null) {
                throw new RuntimeException(annotationType.getSimpleName()+" annotation must have exactly one of targetName, targetClass, or targetProvider");
            }

            targetClassHandle = conDynUtils().conDynFromClass(targetClass);
        }
        if (targetFunction != null) {
            if (targetClassHandle != null) {
                throw new RuntimeException(annotationType.getSimpleName()+" annotation must have exactly one of targetName, targetClass, or targetProvider");
            }

            targetClassHandle = conDynUtils().conDynFromFunction(targetFunction, targetName);
        }

        return targetClassHandle;
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
    public List<MethodParameter<Type, AnnotationTree>> parameters(MethodTree method, @Nullable Class<?> type) {
        return method.getParameters().stream().map(variableTree -> {
            AnnotationTree[] found = new AnnotationTree[1];
            variableTree.getModifiers().getAnnotations().forEach(a -> {
                try {
                    AnnotationMirror annotation = (AnnotationMirror) Utils.JC_ANNOTATION_GET_ATTRIBUTE.invoke(a);
                    var binaryName = elements.getBinaryName((TypeElement) annotation.getAnnotationType().asElement());
                    if (type != null && binaryName.contentEquals(type.getName())) {
                        if (found[0] != null) {
                            throw new RuntimeException("Method " + method.getName() + " parameter " + variableTree.getName() + " may have at most one annotation of type " + type.getSimpleName() + ", but had more than one");
                        }
                        found[0] = a;
                    }
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            });
            Type paramType = types().type(typeFromTree(variableTree.getType()));
            return new MethodParameter<>(paramType, found[0]);
        }).toList();
    }

    @Override
    public Open.Type type(AnnotationTree annotation) {
        Tree name = findAnnotationArgument(annotation, "type");
        if (name instanceof MemberSelectTree memberSelectTree) {
            return Open.Type.valueOf(memberSelectTree.getIdentifier().toString());
        }
        throw new RuntimeException("Could not find type argument in annotation "+annotation);
    }

    @Override
    public @Nullable String name(AnnotationTree annotation) {
        Tree name = findAnnotationArgument(annotation, "name");
        if (name instanceof LiteralTree literalTree) {
            return (String) literalTree.getValue();
        }
        return null;
    }

    @Override
    public boolean unsafe(AnnotationTree annotation) {
        Tree unsafe = findAnnotationArgument(annotation, "unsafe");
        if (unsafe instanceof LiteralTree literalTree) {
            return literalTree.getValue().equals(true);
        }
        return false;
    }

    private @Nullable Tree findAnnotationArgument(AnnotationTree annotation, String name) {
        for (ExpressionTree argument : annotation.getArguments()) {
            if (argument instanceof AssignmentTree assignmentTree) {
                if (assignmentTree.getVariable().toString().equals(name)) {
                    return assignmentTree.getExpression();
                }
            }
        }
        return null;
    }

    @Override
    public Type returnType(MethodTree method) {
        Tree outType = method.getReturnType();
        return types().type(typeFromTree(outType));
    }

    private String typeFromTree(Tree outType) {
        try {
            if (outType instanceof IdentifierTree identifierTree) {
                TypeElement paramType = (TypeElement) Utils.JC_VARIABLE_GET_SYMBOL.invoke(identifierTree);
                return "L"+elements.getBinaryName(paramType).toString().replace('.','/')+";";
            } else if (outType instanceof ArrayTypeTree arrayTypeTree) {
                return "["+typeFromTree(arrayTypeTree.getType());
            } else {
                PrimitiveTypeTree primitiveTypeTree = (PrimitiveTypeTree) outType;
                return switch (primitiveTypeTree.getPrimitiveTypeKind()) {
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
        return declaringClassType;
    }
}
