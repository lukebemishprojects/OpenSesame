package dev.lukebemish.opensesame.transform


import dev.lukebemish.opensesame.runtime.OpeningMetafactory
import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Handle
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.classgen.BytecodeExpression
import org.codehaus.groovy.classgen.asm.BytecodeHelper
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TransformWithPriority
import org.codehaus.groovy.transform.stc.StaticTypesMarker

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
class OpenSesameWriterTransformation extends AbstractASTTransformation implements TransformWithPriority {
    private static final ClassNode OPENING_METAFACTORY = ClassHelper.makeWithoutCaching(OpeningMetafactory)

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)

        MethodNode methodNode = (MethodNode) nodes[1]

        Set<ClassNode> openedClasses = new HashSet<>(this.getMemberClassList((AnnotationNode) nodes[0], 'value'))

        ClassCodeExpressionTransformer trn = new ClassCodeExpressionTransformer() {
            @Override
            protected SourceUnit getSourceUnit() {
                return source
            }

            @Override
            Expression transform(Expression expr) {
                if (expr instanceof MethodCall) {
                    MethodNode method = ((Expression) expr).getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET)
                    if (method === null) {
                        return super.transform(expr)
                    }
                    if ((method.private ||
                            (method.packageScope && method.declaringClass.package != methodNode.declaringClass.package) ||
                            (method.protected && methodNode.declaringClass.isDerivedFrom(method.declaringClass.declaringClass)))
                            && openedClasses.contains(method.declaringClass)) {
                        boolean isCtor = method.name == '$opensesame$$new'
                        if (isCtor) {
                            var ctor = method.declaringClass.getDeclaredConstructor(method.parameters)
                            if (
                                    ctor !== null &&
                                    !ctor.private &&
                                    (!method.packageScope || method.declaringClass.package == methodNode.declaringClass.package) &&
                                    (!method.protected || !methodNode.declaringClass.isDerivedFrom(method.declaringClass.declaringClass))) {
                                var out = new ConstructorCallExpression(
                                        method.declaringClass,
                                        expr.arguments
                                )
                                for (final key : [StaticTypesMarker.TYPE, StaticTypesMarker.INFERRED_TYPE, StaticTypesMarker.INFERRED_RETURN_TYPE]) {
                                    var old = ((Expression) expr).getNodeMetaData(key)
                                    if (old !== null) {
                                        out.setNodeMetaData(key, old)
                                    }
                                }
                                out.setNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET, ctor)
                                return out
                            }
                        }
                        String outMethodPart = "\$\$${method.declaringClass.name.replace('.','$')}\$\$${method.name}\$\$${method.parameters.collect {it.type.name.replace('.','$')}.join('$')}"
                        String bridgeMethodName = "\$opensesame_bridge${outMethodPart}"
                        Parameter[] parameters = new Parameter[method.static ? method.parameters.size() : method.parameters.size() + 1]
                        if (!method.static) {
                            parameters[0] = new Parameter(method.declaringClass, "this")
                            for (int i = 0; i < method.parameters.size(); i++) {
                                parameters[i + 1] = method.parameters[i]
                            }
                        } else {
                            for (int i = 0; i < method.parameters.size(); i++) {
                                parameters[i] = method.parameters[i]
                            }
                        }
                        MethodNode bridgeMethod = new MethodNode(
                                bridgeMethodName,
                                Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                method.returnType,
                                parameters,
                                method.exceptions,
                                new ExpressionStatement(new BytecodeExpression(method.returnType) {

                                    @Override
                                    void visit(MethodVisitor methodVisitor) {
                                        if (!method.static) {
                                            BytecodeHelper.load(methodVisitor, method.declaringClass, 0)
                                        }
                                        int offset = method.static ? 0 : 1
                                        for (int i = 0; i < method.parameters.size(); i++) {
                                            var pType = method.parameters[i].type
                                            BytecodeHelper.load(methodVisitor, pType, i + offset)
                                        }
                                        var methodName = 'invokeInstance'
                                        if (method.static)
                                            methodName = 'invokeStatic'
                                        if (isCtor)
                                            methodName = 'invokeCtor'
                                        methodVisitor.visitInvokeDynamicInsn(
                                                method.name,
                                                BytecodeHelper.getMethodDescriptor(method.returnType, parameters),
                                                new Handle(
                                                        Opcodes.H_INVOKESTATIC,
                                                        BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                                                        methodName,
                                                        BytecodeHelper.getMethodDescriptor(OPENING_METAFACTORY.getMethods(methodName)[0]),
                                                        false
                                                ),
                                                Type.getObjectType(BytecodeHelper.getClassInternalName(method.declaringClass)),
                                        )
                                        if (method.returnType == ClassHelper.VOID_TYPE) {
                                            methodVisitor.visitInsn(Opcodes.ACONST_NULL)
                                        }
                                    }
                                })
                        )
                        methodNode.declaringClass.addMethod(bridgeMethod)

                        List<Expression> outParameters = new ArrayList<>()
                        if (!method.static) {
                            outParameters.add((Expression) expr.receiver)
                        }
                        outParameters.addAll(((ArgumentListExpression) expr.arguments).expressions)
                        var classExpression = new ClassExpression(methodNode.declaringClass)
                        classExpression.setNodeMetaData(StaticTypesMarker.INFERRED_TYPE, ClassHelper.makeWithoutCaching(Class).tap {
                            it.setGenericsTypes(new GenericsType[]{new GenericsType(methodNode.declaringClass)})
                        })
                        var out = new MethodCallExpression(
                                classExpression,
                                bridgeMethodName,
                                new ArgumentListExpression(
                                        outParameters
                                )
                        )

                        out.setNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET, bridgeMethod)
                        for (final key : [StaticTypesMarker.TYPE, StaticTypesMarker.INFERRED_TYPE, StaticTypesMarker.INFERRED_RETURN_TYPE]) {
                            var old = ((Expression) expr).getNodeMetaData(key)
                            if (old !== null) {
                                out.setNodeMetaData(key, old)
                            }
                        }
                        out.setMethodTarget(bridgeMethod)

                        return out
                    }
                }
                return super.transform(expr)
            }
        }

        trn.visitMethod(methodNode)
    }

    @Override
    int priority() {
        return -1
    }
}
