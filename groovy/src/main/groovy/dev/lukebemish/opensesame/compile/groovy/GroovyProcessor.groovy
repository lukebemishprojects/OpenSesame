package dev.lukebemish.opensesame.compile.groovy

import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.compile.ConDynUtils
import dev.lukebemish.opensesame.compile.Processor
import dev.lukebemish.opensesame.compile.TypeProvider
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import groovyjarjarasm.asm.Handle
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.classgen.asm.BytecodeHelper
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.jetbrains.annotations.Nullable

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.function.Function

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@PackageScope(PackageScopeTarget.CLASS)
class GroovyProcessor implements Processor<Type, AnnotationNode, MethodNode> {
    private static final ClassNode GENERIC_CLASS = ClassHelper.makeWithoutCaching(Class).getPlainNodeReference().tap {
        it.setGenericsTypes(new GenericsType[] {new GenericsType(ClassHelper.OBJECT_TYPE).tap {
            it.wildcard = true
        }})
    }
    private static final ClassNode CLASSLOADER = ClassHelper.makeWithoutCaching(ClassLoader)
    private static final String METHOD_CLOSURE_COUNT_META = 'dev.lukebemish.opensesame:closureCount'

    final AbstractASTTransformation transformation

    GroovyProcessor(AbstractASTTransformation transformation) {
        this.transformation = transformation
    }

    @Override
    TypeProvider<Type, ?, ?> types() {
        return GroovyASMTypeProvider.INSTANCE
    }

    @Override
    ConDynUtils<Type, ?, ?> conDynUtils() {
        return GroovyASMTypeProvider.CON_DYN_UTILS
    }

    ConDynUtils.TypedDynamic<?, Type> typeProviderFromAnnotation(AnnotationNode annotationNode, Object nodeContext, Class<?> annotationType) {
        var methodOrTypeNode = (AnnotatedNode) nodeContext
        String nodeName
        ClassNode declaringClass
        if (methodOrTypeNode instanceof MethodNode) {
            nodeName = methodOrTypeNode.name
            declaringClass = methodOrTypeNode.declaringClass
        } else if (methodOrTypeNode instanceof ClassNode) {
            nodeName = methodOrTypeNode.name
            declaringClass = methodOrTypeNode
        } else {
            throw new RuntimeException("Expected either a MethodNode or a ClassNode, but got ${methodOrTypeNode.getClass().simpleName}")
        }

        ConDynUtils.TypedDynamic<?, Type> targetClassHandle = null

        var targetName = transformation.getMemberStringValue(annotationNode, 'targetName')
        var targetClass = transformation.getMemberClassValue(annotationNode, 'targetClass')
        var targetFunction = transformation.getMemberClassValue(annotationNode, 'targetProvider')
        Object targetClosureHandle = null
        if (targetFunction == null) {
            Expression member = annotationNode.getMember('targetProvider')
            if (member instanceof ClosureExpression) {
                if (member.parameters !== null && member.parameters.length >= 1 && member.parameters[0].type != ClassHelper.OBJECT_TYPE && member.parameters[0].type != CLASSLOADER) {
                    throw new RuntimeException("First parameter of closure passed to ${annotationType.simpleName}, if it exists, must be either an Object or a ClassLoader")
                }
                if (member.parameters !== null && member.parameters.length >= 2 && member.parameters[1].type != ClassHelper.OBJECT_TYPE && member.parameters[1].type != ClassHelper.STRING_TYPE) {
                    throw new RuntimeException("First parameter of closure passed to ${annotationType.simpleName}, if it exists, must be either an Object or a String")
                }

                int count = (int) (methodOrTypeNode.getNodeMetaData(METHOD_CLOSURE_COUNT_META) ?: 0)

                String generatedMethodName = "\$dev_lukebemish_opensesame\$typeFinding\$${count++}\$_${nodeName}"

                var pName1 = member.parameters === null || member.parameters.length <= 0 ? 'it' : member.parameters[0].name
                var pName2 = member.parameters === null || member.parameters.length <= 1 ? 'not$$$'+pName1 : member.parameters[1].name

                var generatedMethod = declaringClass.addMethod(
                        generatedMethodName,
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                        GENERIC_CLASS,
                        new Parameter[]{
                                new Parameter(CLASSLOADER, pName1),
                                new Parameter(ClassHelper.STRING_TYPE, pName2)
                        },
                        new ClassNode[]{},
                        member.code
                )

                methodOrTypeNode.setNodeMetaData(METHOD_CLOSURE_COUNT_META, count)

                generatedMethod.synthetic = true

                targetClosureHandle = new Handle(
                        Opcodes.H_INVOKESTATIC,
                        BytecodeHelper.getClassInternalName(declaringClass),
                        generatedMethodName,
                        MethodType.methodType(Class, ClassLoader, String).descriptorString(),
                        declaringClass.interface
                )

                targetClosureHandle = conDynUtils().invoke(
                        MethodHandle.class.descriptorString(),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                Type.getInternalName(MethodHandles.class),
                                "insertArguments",
                                MethodType.methodType(MethodHandle.class, MethodHandle.class, int.class, Object[].class).descriptorString(),
                                false
                        ),
                        targetClosureHandle,
                        1,
                        targetName == null ? conDynUtils().makeNull(Type.getType(String)) : targetName
                )

                annotationNode.members.remove('targetProvider')
            }
        }
        if (targetName === null && targetClass === null && targetFunction === null && targetClosureHandle === null) {
            throw new RuntimeException("${annotationType.simpleName} annotation must have exactly one of targetName, targetClass, or targetProvider")
        } else if (targetName !== null && targetFunction === null) {
            targetClassHandle = conDynUtils().conDynFromName(targetName, Function.identity())
        }
        if (targetClass !== null) {
            if (targetClassHandle !== null) {
                throw new RuntimeException("${annotationType.simpleName} annotation must have exactly one of targetName, targetClass, or targetProvider")
            }

            targetClassHandle = conDynUtils().conDynFromClass(types().type(BytecodeHelper.getTypeDescription(targetClass)))
        }
        if (targetFunction !== null) {
            if (targetClassHandle !== null) {
                throw new RuntimeException("${annotationType.simpleName} annotation must have exactly one of targetName, targetClass, or targetProvider")
            }

            targetClassHandle = conDynUtils().conDynFromFunction(types().type(BytecodeHelper.getTypeDescription(targetFunction)), targetName)
        } else if (targetClosureHandle !== null) {
            targetClassHandle = new ConDynUtils.TypedDynamic<>(targetClosureHandle, null)
        }

        return targetClassHandle
    }

    @Override
    AnnotationNode annotation(MethodNode methodNode, Class<?> type) {
        var members = methodNode.getAnnotations(ClassHelper.makeWithoutCaching(type))
        if (members.size() > 1) {
            throw new RuntimeException("Method ${methodNode.name} may have at most annotation of type ${type.simpleName}, but had more than one")
        } else if (!members.empty) {
            return members.get(0)
        }
        return null
    }

    @Override
    List<MethodParameter<Type, AnnotationNode>> parameters(MethodNode method, @Nullable Class<?> type) {
        return method.parameters.collect {
            AnnotationNode annotation = null
            if (type !== null) {
                var members = it.getAnnotations(ClassHelper.makeWithoutCaching(type))
                if (members.size() > 1) {
                    throw new RuntimeException("Parameter ${it.name} on method ${method.name} may have at most one return type coercion, but had two")
                } else if (!members.empty) {
                    annotation = members.get(0)
                }
            }
            return new MethodParameter<Type, AnnotationNode>(
                    types().type(BytecodeHelper.getTypeDescription(it.type)),
                    annotation
            )
        }
    }

    @Override
    Open.Type type(AnnotationNode annotation) {
        return Open.Type.valueOf((annotation.getMember('type') as PropertyExpression).propertyAsString)
    }

    @Override
    @Nullable String name(AnnotationNode annotation) {
        return transformation.getMemberStringValue(annotation, 'name')
    }

    @Override
    boolean unsafe(AnnotationNode annotation) {
        return transformation.getMemberValue(annotation, 'unsafe')
    }

    @Override
    Type returnType(MethodNode method) {
        return types().type(BytecodeHelper.getTypeDescription(method.returnType))
    }

    @Override
    boolean isStatic(MethodNode method) {
        return method.static
    }

    @Override
    String methodName(MethodNode method) {
        return method.name
    }

    @Override
    Type declaringClass(MethodNode method) {
        return types().type(BytecodeHelper.getTypeDescription(method.declaringClass))
    }
}
