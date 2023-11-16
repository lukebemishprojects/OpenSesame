package dev.lukebemish.opensesame.compile.groovy

import dev.lukebemish.opensesame.annotations.Coerce
import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.runtime.OpeningMetafactory
import groovy.transform.CompileStatic
import groovyjarjarasm.asm.*
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

import java.lang.invoke.*
import java.util.function.Function

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OpenTransformation extends AbstractASTTransformation {
    private static final ClassNode OPEN = ClassHelper.makeWithoutCaching(Open)
    private static final ClassNode COERCE = ClassHelper.makeWithoutCaching(Coerce)
    private static final ClassNode OPENING_METAFACTORY = ClassHelper.makeWithoutCaching(OpeningMetafactory)
    private static final ClassNode FUNCTION = ClassHelper.makeWithoutCaching(Function)
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

        var annotationNode = methodNode.getAnnotations(OPEN).get(0)

        Object targetClassHandle = null
        targetClassHandle = typeProviderFromAnnotation(annotationNode, methodNode, Open)

        final String name = getMemberStringValue(annotationNode, 'name')
        final Open.Type type = Open.Type.valueOf((annotationNode.getMember('type') as PropertyExpression).propertyAsString)

        Type asmDescType = Type.getType(BytecodeHelper.getMethodDescriptor(methodNode.returnType, methodNode.parameters))
        Object returnType = GroovyASMTypeProvider.CON_DYN_UTILS.conDynFromClass(asmDescType.returnType.internalName)
        List<Object> parameterTypes = []
        parameterTypes.addAll(asmDescType.argumentTypes.collect { GroovyASMTypeProvider.CON_DYN_UTILS.conDynFromClass(it.internalName) })

        for (int i = 0; i < methodNode.parameters.size(); i++) {
            var parameter = methodNode.parameters[i]
            var coercions = parameter.getAnnotations(COERCE)
            if (coercions.size() > 1) {
                throw new RuntimeException("Method ${methodNode.name} may have at most one return type coercion, but had two")
            } else if (!coercions.empty) {
                parameterTypes[i] = typeProviderFromAnnotation(coercions.get(0), methodNode, Coerce)
            }
        }

        if (!methodNode.static) {
            var takesInstance = (type == Open.Type.GET_INSTANCE || type == Open.Type.SET_INSTANCE || type == Open.Type.VIRTUAL || type == Open.Type.SPECIAL)

            if (!takesInstance) {
                throw new RuntimeException("Method ${methodNode.name} is not static, but ${Open.simpleName} expects a static context")
            }
            asmDescType = Type.getMethodType(
                    asmDescType.returnType,
                    (new Type[] {Type.getType("L${methodNode.declaringClass.name.replace('.','/')};")}) + asmDescType.argumentTypes
            )
            parameterTypes.add(0, targetClassHandle)
        }

        var coercions = methodNode.getAnnotations(COERCE)
        if (coercions.size() > 1) {
            throw new RuntimeException("Method ${methodNode.name} may have at most one return type coercion, but had two")
        } else if (!coercions.empty) {
            returnType = typeProviderFromAnnotation(coercions.get(0), methodNode, Coerce)
        }

        if (type == Open.Type.CONSTRUCT) {
            returnType = targetClassHandle
        }

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

                var methodType = type.ordinal()

                methodVisitor.visitInvokeDynamicInsn(
                        type == Open.Type.CONSTRUCT ? OpenClassTypeCheckingExtension.CTOR_DUMMY : name,
                        asmDescType.descriptor,
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                                'invoke',
                                Type.getMethodDescriptor(Type.getType(CallSite), Type.getType(MethodHandles.Lookup), Type.getType(String), Type.getType(MethodType), Type.getType(MethodHandle), Type.getType(MethodHandle), Type.getType(int.class)),
                                false
                        ),
                        targetClassHandle,
                        GroovyASMTypeProvider.CON_DYN_UTILS.conDynMethodType(returnType, parameterTypes),
                        methodType
                )

                if (asmDescType.sort == Type.VOID) {
                    methodVisitor.visitInsn(Opcodes.ACONST_NULL)
                }
            }
        })
    }

    private Object typeProviderFromAnnotation(AnnotationNode annotationNode, MethodNode methodNode, Class<?> annotationType) {
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
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                        GENERIC_CLASS,
                        new Parameter[]{
                                new Parameter(CLASSLOADER, 'it')
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
            targetClassHandle = GroovyASMTypeProvider.CON_DYN_UTILS.conDynFromName(targetName)
        }
        if (targetClass !== null) {
            if (targetClassHandle !== null) {
                throw new RuntimeException("${annotationType.simpleName} annotation must have exactly one of targetName, targetClass, or targetProvider")
            }

            targetClassHandle = GroovyASMTypeProvider.CON_DYN_UTILS.conDynFromClass(targetClass.name)
        }
        if (targetFunction !== null) {
            if (targetClassHandle !== null) {
                throw new RuntimeException("${annotationType.simpleName} annotation must have exactly one of targetName, targetClass, or targetProvider")
            }

            targetClassHandle = GroovyASMTypeProvider.CON_DYN_UTILS.conDynFromFunction(targetFunction.name)
        } else if (targetClosureHandle !== null) {
            targetClassHandle = targetClosureHandle
        }

        return targetClassHandle
    }
}
