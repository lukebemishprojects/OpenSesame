package dev.lukebemish.opensesame.compile.groovy

import dev.lukebemish.opensesame.annotations.Coerce
import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.compile.ConDynUtils
import dev.lukebemish.opensesame.compile.OpenProcessor
import dev.lukebemish.opensesame.compile.TypeProvider
import dev.lukebemish.opensesame.runtime.OpeningMetafactory
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import groovyjarjarasm.asm.Handle
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.classgen.BytecodeExpression
import org.codehaus.groovy.classgen.asm.BytecodeHelper
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@PackageScope(PackageScopeTarget.CLASS)
class OpenTransformation extends AbstractASTTransformation implements OpenProcessor<Type, AnnotationNode, MethodNode> {
    private static final ClassNode OPEN = ClassHelper.makeWithoutCaching(Open)
    private static final ClassNode COERCE = ClassHelper.makeWithoutCaching(Coerce)
    private static final ClassNode OPENING_METAFACTORY = ClassHelper.makeWithoutCaching(OpeningMetafactory)
    private static final ClassNode GENERIC_CLASS = ClassHelper.makeWithoutCaching(Class).getPlainNodeReference().tap {
        it.setGenericsTypes(new GenericsType[] {new GenericsType(ClassHelper.OBJECT_TYPE).tap {
            it.wildcard = true
        }})
    }
    private static final ClassNode CLASSLOADER = ClassHelper.makeWithoutCaching(ClassLoader)
    private static final String METHOD_CLOSURE_COUNT_META = 'dev.lukebemish.opensesame:closureCount'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)

        MethodNode methodNode = (MethodNode) nodes[1]
        if (methodNode.getAnnotations(OPEN).size() != 1) {
            throw new RuntimeException("${Open.simpleName} annotation can only be used once per method")
        }

        Opening<Type> opening = opening(methodNode)

        methodNode.code = new ExpressionStatement(new BytecodeExpression(methodNode.returnType) {
            @Override
            void visit(MethodVisitor methodVisitor) {
                if (!methodNode.static) {
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
                }

                for (int i = 0; i < methodNode.parameters.size(); i++) {
                    Type parameterType = Type.getType(BytecodeHelper.getTypeDescription(methodNode.parameters[i].type))
                    methodVisitor.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), methodNode.static ? i : i + 1)
                }

                var methodType = opening.type().ordinal()

                methodVisitor.visitInvokeDynamicInsn(
                        opening.type() == Open.Type.CONSTRUCT ? OpenClassTypeCheckingExtension.CTOR_DUMMY : opening.name(),
                        opening.factoryType().descriptor,
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                                'invoke',
                                Type.getMethodDescriptor(Type.getType(CallSite), Type.getType(MethodHandles.Lookup), Type.getType(String), Type.getType(MethodType), Type.getType(MethodHandle), Type.getType(MethodHandle), Type.getType(int.class)),
                                false
                        ),
                        opening.targetProvider(),
                        opening.methodTypeProvider(),
                        methodType
                )

                if (opening.factoryType().returnType.sort == Type.VOID) {
                    methodVisitor.visitInsn(Opcodes.ACONST_NULL)
                }
            }
        })
    }

    @Override
    TypeProvider<Type, ?, ?> types() {
        return GroovyASMTypeProvider.INSTANCE
    }

    @Override
    ConDynUtils<Type, ?, ?> conDynUtils() {
        return GroovyASMTypeProvider.CON_DYN_UTILS
    }

    Object typeProviderFromAnnotation(AnnotationNode annotationNode, MethodNode methodNode, Class<?> annotationType) {
        Object targetClassHandle = null

        var targetName = getMemberStringValue(annotationNode, 'targetName')
        var targetClass = getMemberClassValue(annotationNode, 'targetClass')
        var targetFunction = getMemberClassValue(annotationNode, 'targetProvider')
        Handle targetClosureHandle = null
        if (targetFunction == null) {
            Expression member = annotationNode.getMember('targetProvider')
            if (member instanceof ClosureExpression) {
                if (member.parameters.length !== 0 && member.parameters[0].type != ClassHelper.OBJECT_TYPE && member.parameters[0].type != CLASSLOADER) {
                    throw new RuntimeException("Closure passed to ${annotationType.simpleName} must have no parameters or a single parameter of type Object or ClassLoader")
                }

                int count = (int) (methodNode.getNodeMetaData(METHOD_CLOSURE_COUNT_META) ?: 0)

                String generatedMethodName = "\$dev_lukebemish_opensesame\$typeFinding\$${count++}\$_${methodNode.name}"

                var generatedMethod = methodNode.declaringClass.addMethod(
                        generatedMethodName,
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                        GENERIC_CLASS,
                        new Parameter[]{
                                new Parameter(CLASSLOADER, member.parameters.length === 0 ? 'it' : member.parameters[0].name)
                        },
                        new ClassNode[]{},
                        member.code
                )

                methodNode.setNodeMetaData(METHOD_CLOSURE_COUNT_META, count)

                generatedMethod.synthetic = true

                targetClosureHandle = new Handle(
                        Opcodes.H_INVOKESTATIC,
                        BytecodeHelper.getClassInternalName(methodNode.declaringClass),
                        generatedMethodName,
                        MethodType.methodType(Class, ClassLoader).descriptorString(),
                        methodNode.declaringClass.interface
                )

                annotationNode.members.remove('targetProvider')
            }
        }
        if (targetName === null && targetClass === null && targetFunction === null && targetClosureHandle === null) {
            throw new RuntimeException("${annotationType.simpleName} annotation must have exactly one of targetName, targetClass, or targetProvider")
        } else if (targetName !== null) {
            targetClassHandle = conDynUtils().conDynFromName(targetName)
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

            targetClassHandle = conDynUtils().conDynFromFunction(types().type(BytecodeHelper.getTypeDescription(targetFunction)))
        } else if (targetClosureHandle !== null) {
            targetClassHandle = targetClosureHandle
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
    List<MethodParameter<Type, AnnotationNode>> parameters(MethodNode method, Class<?> type) {
        return method.parameters.collect {
            AnnotationNode annotation = null
            var members = it.getAnnotations(ClassHelper.makeWithoutCaching(type))
            if (members.size() > 1) {
                throw new RuntimeException("Parameter ${it.name} on method ${method.name} may have at most one return type coercion, but had two")
            } else if (!members.empty) {
                annotation = members.get(0)
            }
            return new MethodParameter<Type, AnnotationNode>(
                    types().type(BytecodeHelper.getTypeDescription(it.type)),
                    annotation
            )
        }
    }

    @Override
    Open.Type type(AnnotationNode annotation) {
        if (annotation.classNode != OPEN) {
            throw new RuntimeException("Attempted to get type from non-${Open.simpleName} annotation ${annotation.toString()}")
        }
        return Open.Type.valueOf((annotation.getMember('type') as PropertyExpression).propertyAsString)
    }

    @Override
    String name(AnnotationNode annotation) {
        if (annotation.classNode != OPEN) {
            throw new RuntimeException("Attempted to get name from non-${Open.simpleName} annotation ${annotation.toString()}")
        }
        return getMemberStringValue(annotation, 'name')
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
