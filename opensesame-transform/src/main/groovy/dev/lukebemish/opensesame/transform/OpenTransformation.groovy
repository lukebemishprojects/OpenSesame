package dev.lukebemish.opensesame.transform

import dev.lukebemish.opensesame.Coerce
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
    private static final ClassNode OPEN = ClassHelper.makeWithoutCaching(Open)
    private static final ClassNode COERCE = ClassHelper.makeWithoutCaching(Coerce)
    private static final ClassNode OPENING_METAFACTORY = ClassHelper.makeWithoutCaching(OpeningMetafactory)

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)

        MethodNode methodNode = (MethodNode) nodes[1]
        if (methodNode.getAnnotations(OPEN).size() != 1) {
            throw new RuntimeException("Opener annotation can only be used once per method")
        }

        if (!methodNode.static) {
            throw new RuntimeException("Opener annotation can only be used on static methods")
        }

        var annotationNode = methodNode.getAnnotations(OPEN).get(0)

        final String target = getMemberStringValue(annotationNode, 'target')
        final String name = getMemberStringValue(annotationNode, 'name')
        final String desc = BytecodeHelper.getMethodDescriptor(methodNode.returnType, methodNode.parameters)
        final Open.Type type = Open.Type.valueOf((annotationNode.getMember('type') as PropertyExpression).propertyAsString)
        final List<String> modules = getMemberStringList(annotationNode, 'module') ?: []

        Type asmDescType = Type.getType(desc)
        Type returnType = asmDescType.returnType
        List<Type> parameterTypes = []
        parameterTypes.addAll(asmDescType.argumentTypes)

        for (int i = 0; i < methodNode.parameters.size(); i++) {
            var parameter = methodNode.parameters[i]
            var coercions = parameter.getAnnotations(COERCE)
            if (coercions.size() > 1) {
                throw new RuntimeException("Method ${methodNode.name} may have at most one return type coercion, but had two")
            } else if (!coercions.empty) {
                parameterTypes[i] = Type.getType(getMemberStringValue(coercions.get(0), 'value'))
            }
        }

        var coercions = methodNode.getAnnotations(COERCE)
        if (coercions.size() > 1) {
            throw new RuntimeException("Method ${methodNode.name} may have at most one return type coercion, but had two")
        } else if (!coercions.empty) {
            returnType = Type.getType(getMemberStringValue(coercions.get(0), 'value'))
        }

        if (type == Open.Type.CONSTRUCT) {
            returnType = Type.getType("L${target.replace('.','/')};")
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
                        desc,
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
