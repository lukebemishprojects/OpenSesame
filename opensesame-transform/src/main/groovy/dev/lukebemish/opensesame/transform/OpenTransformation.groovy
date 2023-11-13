package dev.lukebemish.opensesame.transform

import dev.lukebemish.opensesame.Open
import dev.lukebemish.opensesame.runtime.OpeningMetafactory
import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Handle
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.classgen.BytecodeExpression
import org.codehaus.groovy.classgen.asm.BytecodeHelper
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OpenTransformation extends AbstractASTTransformation {
    private static final ClassNode OPENER = ClassHelper.makeWithoutCaching(Open)
    private static final ClassNode OPENING_METAFACTORY = ClassHelper.makeWithoutCaching(OpeningMetafactory)

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)

        MethodNode methodNode = (MethodNode) nodes[1]
        if (methodNode.getAnnotations(OPENER).size() != 1) {
            throw new RuntimeException("Opener annotation can only be used once per method")
        }

        if (!methodNode.static) {
            throw new RuntimeException("Opener annotation can only be used on static methods")
        }

        var annotationNode = methodNode.getAnnotations(OPENER).get(0)

        final String target = getMemberStringValue(annotationNode, 'target')
        final String name = getMemberStringValue(annotationNode, 'name')
        final String desc = getMemberStringValue(annotationNode, 'desc')
        final Open.Type type = Open.Type.valueOf((annotationNode.getMember('type') as PropertyExpression).propertyAsString)
        final List<String> modules = getMemberStringList(annotationNode, 'module') ?: []

        if (type.field && desc.startsWith('(')) {
            throw new RuntimeException("Field opener, but provided with method descriptor ${desc}")
        }

        if (!type.field && !desc.startsWith('(')) {
            throw new RuntimeException("Method opener, but provided with field descriptor ${desc}")
        }

        Type asmTarget = Type.getType("L${target.replace('.', '/')};")

        Type asmDescType = Type.getType(desc)
        Type returnType = switch (type) {
            case Open.Type.STATIC, Open.Type.VIRTUAL, Open.Type.SPECIAL -> asmDescType.getReturnType()
            case Open.Type.SET_STATIC, Open.Type.SET_INSTANCE -> Type.VOID_TYPE
            case Open.Type.GET_STATIC, Open.Type.GET_INSTANCE -> asmDescType
            case Open.Type.CONSTRUCT -> asmTarget
        }
        final List<Type> parameterTypes = []
        if (type.takesInstance) {
            parameterTypes.add(asmTarget)
        }
        parameterTypes.addAll(switch (type) {
            case Open.Type.STATIC, Open.Type.VIRTUAL, Open.Type.SPECIAL, Open.Type.CONSTRUCT -> asmDescType.getArgumentTypes()
            case Open.Type.SET_STATIC, Open.Type.SET_INSTANCE -> [asmDescType]
            default -> []
        } as Collection<Type>)

        if (methodNode.parameters.length != parameterTypes.size()) {
            throw new RuntimeException("Method ${methodNode.name} has ${methodNode.parameters.length} parameters, but the generated opener expects ${parameterTypes.size()}")
        }

        methodNode.code = new ExpressionStatement(new BytecodeExpression(methodNode.returnType) {
            @Override
            void visit(MethodVisitor methodVisitor) {
                for (int i = 0; i < parameterTypes.size(); i++) {
                    methodVisitor.visitVarInsn(parameterTypes[i].getOpcode(Opcodes.ILOAD), i)
                }

                var methodType = type.ordinal()

                methodVisitor.visitInvokeDynamicInsn(
                        type == Open.Type.CONSTRUCT ? 'constructor' : name,
                        BytecodeHelper.getMethodDescriptor(methodNode.returnType, methodNode.parameters),
                        new Handle(
                                Opcodes.H_INVOKESTATIC,
                                BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                                'invoke',
                                Type.getMethodDescriptor(Type.getType(CallSite), Type.getType(MethodHandles.Lookup), Type.getType(String), Type.getType(MethodType), Type.getType(String), Type.getType(String), Type.getType(String), Type.getType(int.class)),
                                false
                        ),
                        target,
                        Type.getMethodDescriptor(returnType, parameterTypes.toArray(Type[]::new)),
                        modules.join(';'),
                        methodType
                )

                if (returnType.sort == Type.VOID) {
                    methodVisitor.visitInsn(Opcodes.ACONST_NULL)
                }
            }
        })
    }
}
