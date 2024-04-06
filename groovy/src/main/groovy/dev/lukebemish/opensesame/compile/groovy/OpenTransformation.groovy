package dev.lukebemish.opensesame.compile.groovy


import dev.lukebemish.opensesame.annotations.Open
import dev.lukebemish.opensesame.compile.Processor
import dev.lukebemish.opensesame.runtime.OpeningMetafactory
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.PackageScopeTarget
import groovyjarjarasm.asm.Handle
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
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
class OpenTransformation extends AbstractASTTransformation {
    private static final ClassNode OPEN = ClassHelper.makeWithoutCaching(Open)
    private static final ClassNode OPENING_METAFACTORY = ClassHelper.makeWithoutCaching(OpeningMetafactory)

    private static final ClassNode UNFINAL = ClassHelper.makeWithoutCaching("dev.lukebemish.opensesame.annotations.mixin.UnFinal")

    private final GroovyProcessor processor = new GroovyProcessor(this)

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)

        MethodNode methodNode = (MethodNode) nodes[1]
        if (methodNode.getAnnotations(OPEN).size() != 1) {
            throw new RuntimeException("${Open.simpleName} annotation can only be used once per method")
        }

        Processor.Opening<Type> opening = processor.opening(methodNode)

        if (!methodNode.getAnnotations(UNFINAL).empty) {
            String line
            if (opening.targetType() == null || opening.returnType() == null || opening.parameterTypes().stream().anyMatch(Objects::isNull)) {
                line = null
            } else {
                line = switch (opening.type()) {
                    case Open.Type.STATIC -> opening.name().replace('.', '/') + "." + Type.getMethodType(opening.returnType(), opening.parameterTypes().toArray(Type[]::new)).getDescriptor()
                    case Open.Type.VIRTUAL, Open.Type.SPECIAL -> {
                        List<Type> parameterTypes = new ArrayList<>(opening.parameterTypes())
                        parameterTypes.remove(0)
                        yield opening.name().replace('.', '/') + "." + Type.getMethodType(opening.returnType(), parameterTypes.toArray(Type[]::new)).getDescriptor()
                    }
                    case Open.Type.GET_STATIC, Open.Type.GET_INSTANCE -> opening.name().replace('.', '/') + "." + opening.returnType().getDescriptor()
                    case Open.Type.SET_STATIC, Open.Type.SET_INSTANCE -> opening.name().replace('.', '/') + "." + opening.parameterTypes().get(opening.parameterTypes().size()-1).getDescriptor()
                    default -> null
                }
            }
            if (line != null) {
                List<String> lines = methodNode.declaringClass.getNodeMetaData(Discoverer.MIXIN_LINES_META)
                if (lines == null) {
                    lines = new ArrayList<>()
                    methodNode.declaringClass.putNodeMetaData(Discoverer.MIXIN_LINES_META, lines)
                }
                lines.add(opening.targetType().getInternalName() + "." + line)
            }
        }

        methodNode.code = new ExpressionStatement(new BytecodeExpression(methodNode.returnType) {
            @Override
            void visit(MethodVisitor methodVisitor) {
                if (!methodNode.static) {
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
                }

                int j = 0
                for (int i = 0; i < methodNode.parameters.size(); i++) {
                    Type parameterType = Type.getType(BytecodeHelper.getTypeDescription(methodNode.parameters[i].type))
                    methodVisitor.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), methodNode.static ? j : j + 1)
                    j += parameterType.getSize()
                }

                var methodType = opening.type().ordinal()

                methodVisitor.visitInvokeDynamicInsn(
                        opening.type() == Open.Type.CONSTRUCT ? OpenClassTypeCheckingExtension.CTOR_DUMMY : opening.name(),
                        opening.factoryType().descriptor,
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                                opening.unsafe() ? 'invokeUnsafe' : 'invoke',
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
}
