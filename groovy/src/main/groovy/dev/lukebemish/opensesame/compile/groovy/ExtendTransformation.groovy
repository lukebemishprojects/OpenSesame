package dev.lukebemish.opensesame.compile.groovy

import dev.lukebemish.opensesame.annotations.extend.Constructor
import dev.lukebemish.opensesame.annotations.extend.Extend
import dev.lukebemish.opensesame.annotations.extend.Field
import dev.lukebemish.opensesame.annotations.extend.Overrides
import dev.lukebemish.opensesame.compile.ConDynUtils.TypedDynamic
import dev.lukebemish.opensesame.compile.Processor
import dev.lukebemish.opensesame.runtime.OpeningMetafactory
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import groovyjarjarasm.asm.Handle
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
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
import java.util.function.Consumer

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@PackageScope(PackageScopeTarget.CLASS)
class ExtendTransformation extends AbstractASTTransformation {
    private static final ClassNode EXTEND = ClassHelper.makeWithoutCaching(Extend)
    private static final ClassNode CONSTRUCTOR = ClassHelper.makeWithoutCaching(Constructor)
    private static final ClassNode OVERRIDES = ClassHelper.makeWithoutCaching(Overrides)
    private static final ClassNode FIELD = ClassHelper.makeWithoutCaching(Field)
    private static final ClassNode FIELD_FINAL = ClassHelper.makeWithoutCaching(Field.Final)
    private final GroovyProcessor processor = new GroovyProcessor(this)

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)

        ClassNode classNode = (ClassNode) nodes[1]
        AnnotationNode annotation = (AnnotationNode) nodes[0]
        if (classNode.getAnnotations(EXTEND).size() != 1) {
            throw new RuntimeException("${Extend.simpleName} annotation can only be used once per method")
        }

        TypedDynamic<?, Type> extendTargetClassHandle = processor.typeProviderFromAnnotation(annotation, classNode, Extend.class)
        boolean extendUnsafe = processor.unsafe(annotation)

        Map<String, Processor.ExtendFieldInfo<Type>> fieldMap = new HashMap<>()
        List<Processor.ExtendCtorInfo> ctors = new ArrayList<>()
        List<Processor.ExtendOverrideInfo<Type>> overrides = new ArrayList<>()

        var holderType = Type.getType(BytecodeHelper.getTypeDescription(classNode))

        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.getAnnotations(CONSTRUCTOR).size() > 0) {
                processConstructor(methodNode, holderType, ctors::add, fieldMap, extendUnsafe, extendTargetClassHandle)
            } else if (methodNode.getAnnotations(OVERRIDES).size() > 0) {
                processOverrides(methodNode, overrides::add)
            } else if (methodNode.getAnnotations(FIELD).size() > 0) {
                processField(methodNode, fieldMap)
            }
        }

        GroovyClassAccumulator accumulator = new GroovyClassAccumulator(classNode)
        processor.extensionBytecode(
                accumulator,
                ctors,
                extendTargetClassHandle,
                fieldMap,
                false,
                overrides,
                holderType,
                (type, name, returnTypes, parameterTypes) -> name
        )

        var initialHolderField = classNode.getField(Processor.EXTEND_GENERATED_CLASS)
        initialHolderField.setInitialValueExpression(new ArrayExpression(ClassHelper.makeWithoutCaching(Class.class), List.<Expression>of(new ConstantExpression(1))))
    }

    void processConstructor(MethodNode methodNode, Type holderType, Consumer<Processor.ExtendCtorInfo> ctorConsumer, Map<String, Processor.ExtendFieldInfo<Type>> fieldMap, boolean unsafeExtension, TypedDynamic<?, Type> extendTargetClassHandle) {
        if (!methodNode.static) {
            throw new RuntimeException("@Constructor must be static")
        } else if (methodNode.abstract) {
            throw new RuntimeException("@Constructor cannot be abstract")
        }

        Processor.CoercedDescriptor<Type> descriptor = processor.coercedDescriptor(methodNode)
        if (holderType != descriptor.returnType().type()) {
            throw new RuntimeException("@Constructor must have return type of "+holderType.getClassName())
        }

        var parameterTypes = methodNode.parameters.collect {Type.getType(BytecodeHelper.getTypeDescription(it.type))}

        var fields = methodNode.parameters.collect {it.getAnnotations(FIELD).find()}.collect {it ? processor.name(it) : null}
        var fieldsFinal = methodNode.parameters.collect {it.getAnnotations(FIELD_FINAL).find()}
        int drop = 0
        List<String> fieldNames = new ArrayList<>()
        if (fields != null) {
            boolean finished = false
            for (int i = 0; i < fields.size(); i++) {
                var field = fields[i]
                if (field === null) {
                    finished = true
                    continue
                }
                if (finished) {
                    throw new RuntimeException("@Constructor must have all field parameters before non-field parameters")
                }
                if (fieldNames.contains(field)) {
                    throw new RuntimeException("@Constructor must not have duplicate field parameters")
                }
                drop++
                Type fieldType = parameterTypes[i]
                var setAsFinal = fieldsFinal != null && fieldsFinal[i] != null
                Processor.ExtendFieldInfo<Type> fieldInfo = fieldMap.computeIfAbsent(field, k -> new Processor.ExtendFieldInfo<>(field, fieldType, setAsFinal))
                if (!fieldInfo.isFinal() && setAsFinal) {
                    fieldInfo = new Processor.ExtendFieldInfo<>(field, fieldType, true, fieldInfo.getters(), fieldInfo.setters())
                    fieldMap.put(field, fieldInfo)
                }
                if (fieldInfo.type() != fieldType) {
                    throw new RuntimeException("@Constructor field parameter type must match field type")
                }
                fieldNames.add(field)
            }
        }
        List<TypedDynamic<?, Type>> superCtorTypes = new ArrayList<>(descriptor.parameterTypes())
        if (drop > 0) {
            superCtorTypes.subList(0, drop).clear()
        }
        var voidType = processor.conDynUtils().conDynFromClass(Type.getType(void.class))

        Object superCtorType = processor.conDynUtils().conDynMethodType(voidType.constantDynamic(), superCtorTypes.stream().<Object>map(TypedDynamic::constantDynamic).toList())
        Object ctorType = processor.conDynUtils().conDynMethodType(voidType.constantDynamic(), parameterTypes.collect {(Object) processor.conDynUtils().conDynFromClass(it)})

        ctorConsumer.accept(new Processor.ExtendCtorInfo(ctorType, superCtorType, fieldNames))

        methodNode.code = new ExpressionStatement(new BytecodeExpression(methodNode.returnType) {
            @Override
            void visit(MethodVisitor methodVisitor) {
                int j = 0
                for (Type parameterType : parameterTypes) {
                    methodVisitor.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), j)
                    j += parameterType.getSize()
                }

                var returnType = Type.getType(BytecodeHelper.getTypeDescription(methodNode.returnType))

                methodVisitor.visitInvokeDynamicInsn(
                        methodNode.name,
                        Type.getMethodDescriptor(returnType, parameterTypes.toArray(Type[]::new)),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                Type.getInternalName(OpeningMetafactory.class),
                                unsafeExtension ? "makeOpenClassUnsafe" : "makeOpenClass",
                                MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, MethodHandle.class, MethodHandle.class, MethodHandle.class, MethodHandle.class).toMethodDescriptorString(),
                                false
                        ),
                        extendTargetClassHandle.constantDynamic(),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                holderType.getInternalName(),
                                Processor.EXTEND_GENERATED_CLASS,
                                MethodType.methodType(void.class, Class.class).toMethodDescriptorString(),
                                true
                        ),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                holderType.getInternalName(),
                                Processor.EXTEND_GENERATED_CLASS,
                                MethodType.methodType(Class.class).toMethodDescriptorString(),
                                true
                        ),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                holderType.getInternalName(),
                                Processor.EXTEND_INFO_GENERATED,
                                MethodType.methodType(List.class, ClassLoader.class).toMethodDescriptorString(),
                                true
                        )
                )

                methodVisitor.visitInsn(Opcodes.ARETURN)
            }
        })
    }

    void processOverrides(MethodNode methodNode, Consumer<Processor.ExtendOverrideInfo<Type>> overrideConsumer) {
        if (methodNode.static) {
            throw new RuntimeException("@Overrides must not be static")
        } else if (methodNode.abstract) {
            throw new RuntimeException("@Overrides must not be abstract")
        }
        Processor.CoercedDescriptor<Type> descriptor = processor.coercedDescriptor(methodNode)
        String originalName = processor.name(methodNode.getAnnotations(OVERRIDES).find())
        if (methodNode.name == originalName) {
            throw new RuntimeException("@Overrides must not have the same name as the original method")
        }
        overrideConsumer.accept(new Processor.ExtendOverrideInfo<>(
                methodNode.name,
                processor.conDynUtils().conDynFromClass(Type.getType(BytecodeHelper.getTypeDescription(methodNode.returnType))),
                methodNode.parameters.collect {processor.conDynUtils().conDynFromClass(Type.getType(BytecodeHelper.getTypeDescription(it.type)))},
                originalName,
                descriptor.returnType(),
                descriptor.parameterTypes()
        ))
    }

    void processField(MethodNode methodNode, Map<String, Processor.ExtendFieldInfo<Type>> fieldMap) {
        if (methodNode.static) {
            throw new RuntimeException("@Field must not be static")
        }
        if (methodNode.parameters.size() > 1 || methodNode.static) {
            throw new RuntimeException("@Field getter/setter must have at most one parameter and must not be static")
        }
        var setter = methodNode.parameters.size() == 1
        var parameterTypes = methodNode.parameters.collect {Type.getType(BytecodeHelper.getTypeDescription(it.type))}
        var returnType = Type.getType(BytecodeHelper.getTypeDescription(methodNode.returnType))
        var fieldName = processor.name(methodNode.getAnnotations(FIELD).find())
        var setAsFinal = methodNode.getAnnotations(FIELD_FINAL).find() != null
        if (setter) {
            if (returnType.getSort() != Type.VOID) {
                throw new RuntimeException("@Field setter must have void return type")
            }
            Type fieldType = parameterTypes.get(0)
            Processor.ExtendFieldInfo<Type> fieldInfo = fieldMap.computeIfAbsent(fieldName, k -> new Processor.ExtendFieldInfo<>(fieldName, fieldType, setAsFinal))
            if (!fieldInfo.isFinal() && setAsFinal) {
                fieldInfo = new Processor.ExtendFieldInfo<>(fieldName, fieldType, true, fieldInfo.getters(), fieldInfo.setters())
                fieldMap.put(fieldName, fieldInfo)
            }
            fieldInfo.setters().add(methodNode.name)
        } else {
            if (returnType.getSort() == Type.VOID) {
                throw new RuntimeException("@Field getter must not have void return type")
            }
            Type fieldType = returnType
            Processor.ExtendFieldInfo<Type> fieldInfo = fieldMap.computeIfAbsent(fieldName, k -> new Processor.ExtendFieldInfo<>(fieldName, fieldType, setAsFinal))
            if (!fieldInfo.isFinal() && setAsFinal) {
                fieldInfo = new Processor.ExtendFieldInfo<>(fieldName, fieldType, true, fieldInfo.getters(), fieldInfo.setters())
                fieldMap.put(fieldName, fieldInfo)
            }
            fieldInfo.getters().add(methodNode.name)
        }
    }
}
