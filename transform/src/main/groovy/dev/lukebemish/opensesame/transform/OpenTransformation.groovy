package dev.lukebemish.opensesame.transform

import dev.lukebemish.opensesame.Coerce
import dev.lukebemish.opensesame.Open
import dev.lukebemish.opensesame.runtime.ErrorFunction
import dev.lukebemish.opensesame.runtime.OpeningMetafactory
import groovy.transform.CompileStatic
import groovyjarjarasm.asm.*
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ClassExpression
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
    private static final ClassNode ERROR_FUNCTION = ClassHelper.makeWithoutCaching(ErrorFunction)
    private static final String METHOD_CLOSURE_COUNT_META = 'dev.lukebemish.opensesame:closureCount'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)

        MethodNode methodNode = (MethodNode) nodes[1]
        if (methodNode.getAnnotations(OPEN).size() != 1) {
            throw new RuntimeException("${Open.simpleName} annotation can only be used once per method")
        }

        var annotationNode = methodNode.getAnnotations(OPEN).get(0)

        String target = null
        Object targetClassHandle = null
        var targetName = getMemberStringValue(annotationNode, 'targetName')
        var targetClass = getMemberClassValue(annotationNode, 'targetClass')
        var targetFunction = getMemberClassValue(annotationNode, 'targetProvider')
        Handle targetClosureHandle = null
        if (targetFunction == null) {
            Expression member = annotationNode.getMember('targetProvider')
            if (member instanceof ClosureExpression) {
                if (member.parameters.length !== 0 && member.parameters[0].type != ClassHelper.OBJECT_TYPE && member.parameters[0].type != CLASSLOADER) {
                    throw new RuntimeException("Closure passed to ${Open.simpleName} must have no parameters or a single parameter of type Object or ClassLoader")
                }

                int count = (int) (methodNode.getNodeMetaData(METHOD_CLOSURE_COUNT_META) ?: 0)

                String generatedMethodName = "\$dev_lukebemish_opensesame\$typeFinding\$${count++}\$_${methodNode.name}"

                var generatedMethod = methodNode.declaringClass.addMethod(
                        generatedMethodName,
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                        GENERIC_CLASS,
                        new Parameter[] {
                                new Parameter(CLASSLOADER, 'it')
                        },
                        new ClassNode[] {},
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

                annotationNode.setMember('targetProvider', new ClassExpression(ERROR_FUNCTION))
            }
        }
        if (targetName === null && targetClass === null && targetFunction === null && targetClosureHandle === null) {
            throw new RuntimeException("${Open.simpleName} annotation must have exactly one of targetName, targetClass, or targetProvider")
        } else if (targetName !== null) {
            target = targetName
            targetClassHandle = conDynFromName(targetName)
        }
        if (targetClass !== null) {
            if (targetClassHandle !== null) {
                throw new RuntimeException("${Open.simpleName} annotation must have exactly one of targetName, targetClass, or targetProvider")
            }

            target = targetClass.name

            targetClassHandle = conDynFromClass(targetClass)
        }
        if (targetFunction !== null) {
            if (targetClassHandle !== null) {
                throw new RuntimeException("${Open.simpleName} annotation must have exactly one of targetName, targetClass, or targetProvider")
            }

            targetClassHandle = conDynFromFunction(targetFunction)
        } else if (targetClosureHandle !== null) {
            targetClassHandle = targetClosureHandle
        }

        final String name = getMemberStringValue(annotationNode, 'name')
        final Open.Type type = Open.Type.valueOf((annotationNode.getMember('type') as PropertyExpression).propertyAsString)

        Type targetAsmType = target == null ? Type.getType(Object) : Type.getType("L${target.replace('.','/')};")

        Type asmDescType = Type.getType(BytecodeHelper.getMethodDescriptor(methodNode.returnType, methodNode.parameters))
        Type returnType = asmDescType.returnType
        List<Type> parameterTypes = []
        parameterTypes.addAll(asmDescType.argumentTypes)

        // TODO: make descriptor use ConstantDynamic too, and add provider support to Coerce

        for (int i = 0; i < methodNode.parameters.size(); i++) {
            var parameter = methodNode.parameters[i]
            var coercions = parameter.getAnnotations(COERCE)
            if (coercions.size() > 1) {
                throw new RuntimeException("Method ${methodNode.name} may have at most one return type coercion, but had two")
            } else if (!coercions.empty) {
                parameterTypes[i] = Type.getType(getMemberStringValue(coercions.get(0), 'value'))
            }
        }

        if (!methodNode.static) {
            if (!type.takesInstance) {
                throw new RuntimeException("Method ${methodNode.name} is not static, but ${Open.simpleName} expects a static context")
            }
            asmDescType = Type.getMethodType(
                    asmDescType.returnType,
                    (new Type[] {Type.getType("L${methodNode.declaringClass.name.replace('.','/')};")}) + asmDescType.argumentTypes
            )
            parameterTypes.add(0, targetAsmType)
        }

        var coercions = methodNode.getAnnotations(COERCE)
        if (coercions.size() > 1) {
            throw new RuntimeException("Method ${methodNode.name} may have at most one return type coercion, but had two")
        } else if (!coercions.empty) {
            returnType = Type.getType(getMemberStringValue(coercions.get(0), 'value'))
        }

        if (type == Open.Type.CONSTRUCT) {
            returnType = targetAsmType
        }

        methodNode.code = new ExpressionStatement(new BytecodeExpression(methodNode.returnType) {
            @Override
            void visit(MethodVisitor methodVisitor) {
                for (int i = 0; i < parameterTypes.size(); i++) {
                    methodVisitor.visitVarInsn(parameterTypes[i].getOpcode(Opcodes.ILOAD), i)
                }

                var methodType = type.ordinal()

                methodVisitor.visitInvokeDynamicInsn(
                        type == Open.Type.CONSTRUCT ? OpenClassTypeCheckingExtension.CTOR_DUMMY : name,
                        asmDescType.descriptor,
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                                'invoke',
                                Type.getMethodDescriptor(Type.getType(CallSite), Type.getType(MethodHandles.Lookup), Type.getType(String), Type.getType(MethodType), Type.getType(MethodHandle), Type.getType(String), Type.getType(int.class)),
                                false
                        ),
                        targetClassHandle,
                        Type.getMethodDescriptor(returnType, parameterTypes.toArray(Type[]::new)),
                        methodType
                )

                if (returnType.sort == Type.VOID) {
                    methodVisitor.visitInsn(Opcodes.ACONST_NULL)
                }
            }
        })
    }

    private static ConstantDynamic conDynFromFunction(ClassNode closureNode) {
        var functionCtor = new Handle(
                Opcodes.H_NEWINVOKESPECIAL,
                BytecodeHelper.getClassInternalName(closureNode),
                '<init>',
                MethodType.methodType(void.class).descriptorString(),
                false
        )

        var asFunction = new ConstantDynamic(
                'targetClass',
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(ConstantBootstraps),
                        'invoke',
                        MethodType.methodType(Object, MethodHandles.Lookup, String, Class, MethodHandle, Object[]).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKEVIRTUAL,
                        Type.getInternalName(MethodHandle),
                        'asType',
                        MethodType.methodType(MethodHandle, MethodType).descriptorString(),
                        false
                ),
                functionCtor,
                Type.getMethodType(Type.getType(Function))
        )

        var fetchClass = new ConstantDynamic(
                'targetClass',
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(ConstantBootstraps),
                        'invoke',
                        MethodType.methodType(Object, MethodHandles.Lookup, String, Class, MethodHandle, Object[]).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodHandles),
                        'collectArguments',
                        MethodType.methodType(MethodHandle, MethodHandle, int, MethodHandle).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKEINTERFACE,
                        Type.getInternalName(Function),
                        'apply',
                        MethodType.methodType(Object, Object).descriptorString(),
                        true
                ),
                0,
                asFunction
        )

        return new ConstantDynamic(
                'targetClass',
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(ConstantBootstraps),
                        'invoke',
                        MethodType.methodType(Object, MethodHandles.Lookup, String, Class, MethodHandle, Object[]).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKEVIRTUAL,
                        Type.getInternalName(MethodHandle),
                        'asType',
                        MethodType.methodType(MethodHandle, MethodType).descriptorString(),
                        false
                ),
                fetchClass,
                Type.getMethodType(Type.getType(Class), Type.getType(ClassLoader))
        )
    }

    private static ConstantDynamic conDynFromClass(ClassNode targetClass) {
        Object targetType = Type.getType(BytecodeHelper.getTypeDescription(targetClass))

        if (ClassHelper.isPrimitiveType(targetClass)) {
            targetType = new ConstantDynamic(
                    BytecodeHelper.getTypeDescription(targetClass),
                    Class.class.descriptorString(),
                    new Handle(
                            Opcodes.H_INVOKESTATIC,
                            Type.getInternalName(ConstantBootstraps),
                            'primitiveClass',
                            MethodType.methodType(Class, MethodHandles.Lookup, String, Class).descriptorString(),
                            false
                    )
            )
        }

        return new ConstantDynamic(
                'targetClass',
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(ConstantBootstraps),
                        'invoke',
                        MethodType.methodType(Object, MethodHandles.Lookup, String, Class, MethodHandle, Object[]).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodHandles),
                        'dropArguments',
                        MethodType.methodType(MethodHandle, MethodHandle, int.class, Class[].class).descriptorString(),
                        false
                ),
                new ConstantDynamic(
                        'targetClass',
                        MethodHandle.class.descriptorString(),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                Type.getInternalName(ConstantBootstraps),
                                'invoke',
                                MethodType.methodType(Object, MethodHandles.Lookup, String, Class, MethodHandle, Object[]).descriptorString(),
                                false
                        ),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                Type.getInternalName(MethodHandles),
                                'constant',
                                MethodType.methodType(MethodHandle, Class, Object).descriptorString(),
                                false
                        ),
                        Type.getType(Class.class),
                        targetType
                ),
                0,
                Type.getType(ClassLoader)
        )
    }

    private static ConstantDynamic conDynFromName(String targetName) {
        // String, ClassLoader
        var classLookupFromNameAndClassloader = new ConstantDynamic(
                'targetClass',
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(ConstantBootstraps),
                        'invoke',
                        MethodType.methodType(Object, MethodHandles.Lookup, String, Class, MethodHandle, Object[].class).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodHandles),
                        'insertArguments',
                        MethodType.methodType(MethodHandle, MethodHandle, int.class, Object[].class).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(Class),
                        'forName',
                        MethodType.methodType(Class, String, boolean.class, ClassLoader).descriptorString(),
                        false
                ),
                1,
                new ConstantDynamic(
                        // booleans are fucky in ConstantDynamics. Here's an alternative...
                        'FALSE',
                        Boolean.class.descriptorString(),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                Type.getInternalName(ConstantBootstraps),
                                'getStaticFinal',
                                MethodType.methodType(Object, MethodHandles.Lookup, String, Class, Class).descriptorString(),
                                false
                        ),
                        Type.getType(Boolean)
                )
        )

        var remapper = new Handle(
                Opcodes.H_INVOKESTATIC,
                BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                'remapClass',
                MethodType.methodType(String, String, ClassLoader).descriptorString(),
                false
        )

        var twoClassloaderArguments = new ConstantDynamic(
                'targetClass',
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(ConstantBootstraps),
                        'invoke',
                        MethodType.methodType(Object, MethodHandles.Lookup, String, Class, MethodHandle, Object[].class).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodHandles),
                        'collectArguments',
                        MethodType.methodType(MethodHandle, MethodHandle, int.class, MethodHandle).descriptorString(),
                        false
                ),
                classLookupFromNameAndClassloader,
                0,
                remapper
        )

        var remappingLookup = new ConstantDynamic(
                'targetClass',
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(ConstantBootstraps),
                        'invoke',
                        MethodType.methodType(Object, MethodHandles.Lookup, String, Class, MethodHandle, Object[]).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodHandles),
                        'permuteArguments',
                        MethodType.methodType(MethodHandle, MethodHandle, MethodType, int[].class).descriptorString(),
                        false
                ),
                twoClassloaderArguments,
                Type.getMethodType(Type.getType(Class), Type.getType(String), Type.getType(ClassLoader)),
                0,
                1,
                1
        )

        return new ConstantDynamic(
                'targetClass',
                MethodHandle.class.descriptorString(),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(ConstantBootstraps),
                        'invoke',
                        MethodType.methodType(Object, MethodHandles.Lookup, String, Class, MethodHandle, Object[].class).descriptorString(),
                        false
                ),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        Type.getInternalName(MethodHandles),
                        'insertArguments',
                        MethodType.methodType(MethodHandle, MethodHandle, int.class, Object[].class).descriptorString(),
                        false
                ),
                remappingLookup,
                0,
                targetName
        )
    }
}
