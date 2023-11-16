package dev.lukebemish.opensesame.compile.groovy

import dev.lukebemish.opensesame.annotations.Open
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
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TransformWithPriority
import org.codehaus.groovy.transform.stc.StaticTypesMarker

import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
class OpenClassWriterTransformation extends AbstractASTTransformation implements TransformWithPriority {
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
                    Expression outExpression = attemptTransformMethod(method, expr)
                    if (outExpression === null) {
                        return super.transform(expr)
                    }
                    return outExpression
                }
                if (expr instanceof PropertyExpression) {
                    boolean allowFieldOnly = expr instanceof AttributeExpression

                    boolean isStatic = expr.objectExpression instanceof ClassExpression

                    if (expr.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET) !== null) {
                        return super.transform(expr)
                    }

                    PropertyNode property = expr.objectExpression.type.getProperty(expr.propertyAsString)
                    FieldNode field = expr.objectExpression.type.getField(expr.propertyAsString)

                    if (property === null && field === null) {
                        return super.transform(expr)
                    }

                    if (property !== null && property.getterName != null && !allowFieldOnly) {
                        Expression getterExpression = new MethodCallExpression(
                                expr.objectExpression,
                                property.getterName,
                                new ArgumentListExpression()
                        )
                        getterExpression.type = property.type
                        var getter = property.declaringClass.getMethod(property.getterName, new Parameter[0])
                        getterExpression.setMethodTarget(getter)
                        Expression outExpression = attemptTransformMethod(getter, getterExpression)
                        if (outExpression !== null) {
                            return outExpression
                        }
                    }

                    if (field === null || !openedClasses.contains(field.declaringClass) || (field.static !== isStatic)) {
                        return super.transform(expr)
                    }

                    if (isAccessible(field.declaringClass, methodNode.declaringClass, field.protected, field.public, field.private)) {
                        return super.transform(expr)
                    }

                    String outMethodPart = "\$\$${field.declaringClass.name.replace('.','$')}\$\$${field.name}"
                    String bridgeMethodName = "\$dev_lukebemish_opensesame_bridge_getter${outMethodPart}"
                    Parameter[] parameters = new Parameter[field.static ? 0 : 1]
                    if (!field.static) {
                        parameters[0] = new Parameter(field.declaringClass, "it")
                    }
                    if (methodNode.declaringClass.getMethods(bridgeMethodName).empty) {
                        MethodNode bridgeMethod = new MethodNode(
                                bridgeMethodName,
                                Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                field.type,
                                parameters,
                                new ClassNode[0],
                                new ExpressionStatement(new BytecodeExpression(field.type) {

                                    @Override
                                    void visit(MethodVisitor methodVisitor) {
                                        if (!field.static) {
                                            BytecodeHelper.load(methodVisitor, field.declaringClass, 0)
                                        }
                                        var methodType = Open.Type.GET_INSTANCE.ordinal()
                                        if (field.static)
                                            methodType = Open.Type.GET_STATIC.ordinal()
                                        methodVisitor.visitInvokeDynamicInsn(
                                                field.name,
                                                BytecodeHelper.getMethodDescriptor(field.type, parameters),
                                                new Handle(
                                                        Opcodes.H_INVOKESTATIC,
                                                        BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                                                        'invoke',
                                                        Type.getMethodDescriptor(Type.getType(CallSite), Type.getType(MethodHandles.Lookup), Type.getType(String), Type.getType(MethodType), Type.getType(Class), Type.getType(int.class)),
                                                        false
                                                ),
                                                Type.getObjectType(BytecodeHelper.getClassInternalName(field.declaringClass)),
                                                methodType
                                        )
                                    }
                                })
                        )
                        methodNode.declaringClass.addMethod(bridgeMethod)
                    }
                    MethodNode bridgeMethod = methodNode.declaringClass.getMethods(bridgeMethodName)[0]

                    List<Expression> outParameters = new ArrayList<>()
                    if (!field.static) {
                        outParameters.add((Expression) expr.objectExpression)
                    }
                    var classExpression = new ClassExpression(methodNode.declaringClass)
                    classExpression.setNodeMetaData(StaticTypesMarker.INFERRED_TYPE, ClassHelper.makeWithoutCaching(Class).tap {
                        it.setGenericsTypes(new GenericsType[]{new GenericsType(methodNode.declaringClass)})
                    })
                    var out = new MethodCallExpression(
                            transform(classExpression),
                            bridgeMethodName,
                            transform(new ArgumentListExpression(
                                    outParameters
                            ))
                    )

                    out.setNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET, bridgeMethod)
                    out.type = field.type
                    out.setMethodTarget(bridgeMethod)

                    return out
                }
                if (expr instanceof BinaryExpression) {
                    if (!Types.isAssignment(expr.operation.type)) {
                        return super.transform(expr)
                    }
                    if (Types.ASSIGN !== expr.operation.type) {
                        //TODO: support other assignment operators
                        expr.rightExpression = super.transform(expr.rightExpression)
                        return expr
                    }
                    Expression lhs = ((BinaryExpression) expr).leftExpression
                    if (lhs instanceof PropertyExpression) {
                        boolean allowFieldOnly = lhs instanceof AttributeExpression

                        boolean isStatic = lhs.objectExpression instanceof ClassExpression

                        if (lhs.getNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET) !== null) {
                            expr.rightExpression = super.transform(expr.rightExpression)
                            return expr
                        }

                        PropertyNode property = lhs.objectExpression.type.getProperty(lhs.propertyAsString)
                        FieldNode field = lhs.objectExpression.type.getField(lhs.propertyAsString)

                        if (property === null && field === null) {
                            expr.rightExpression = super.transform(expr.rightExpression)
                            return expr
                        }

                        if (property !== null && property.setterName != null && !allowFieldOnly) {
                            Expression setterExpression = new MethodCallExpression(
                                    lhs.objectExpression,
                                    property.setterName,
                                    new ArgumentListExpression(new Expression[] {expr.rightExpression})
                            )
                            var setter = property.declaringClass.getMethod(property.setterName, new Parameter[]{new Parameter(expr.rightExpression.type, 'value')})
                            setterExpression.setMethodTarget(setter)
                            Expression outExpression = attemptTransformMethod(setter, setterExpression)
                            if (outExpression !== null) {
                                return outExpression
                            }
                        }

                        if (field === null || !openedClasses.contains(field.declaringClass) || (field.static !== isStatic)) {
                            expr.rightExpression = super.transform(expr.rightExpression)
                            return expr
                        }

                        if (isAccessible(field.declaringClass, methodNode.declaringClass, field.protected, field.public, field.private)) {
                            expr.rightExpression = super.transform(expr.rightExpression)
                            return expr
                        }

                        String outMethodPart = "\$\$${field.declaringClass.name.replace('.', '$')}\$\$${field.name}"
                        String bridgeMethodName = "\$dev_lukebemish_opensesame_bridge_setter${outMethodPart}"
                        Parameter[] parameters = new Parameter[field.static ? 1 : 2]
                        if (!field.static) {
                            parameters[0] = new Parameter(field.declaringClass, "it")
                        }
                        parameters[parameters.length - 1] = new Parameter(expr.rightExpression.type, "value")
                        if (methodNode.declaringClass.getMethods(bridgeMethodName).empty) {
                            MethodNode bridgeMethod = new MethodNode(
                                    bridgeMethodName,
                                    Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                    ClassHelper.VOID_TYPE,
                                    parameters,
                                    new ClassNode[0],
                                    new ExpressionStatement(new BytecodeExpression(field.type) {

                                        @Override
                                        void visit(MethodVisitor methodVisitor) {
                                            if (!field.static) {
                                                BytecodeHelper.load(methodVisitor, field.declaringClass, 0)
                                            }
                                            BytecodeHelper.load(methodVisitor, parameters[parameters.length - 1].type, parameters.length - 1)
                                            var methodType = Open.Type.SET_INSTANCE.ordinal()
                                            if (field.static)
                                                methodType = Open.Type.SET_STATIC.ordinal()
                                            methodVisitor.visitInvokeDynamicInsn(
                                                    field.name,
                                                    BytecodeHelper.getMethodDescriptor(ClassHelper.VOID_TYPE, parameters),
                                                    new Handle(
                                                            Opcodes.H_INVOKESTATIC,
                                                            BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                                                            'invoke',
                                                            Type.getMethodDescriptor(Type.getType(CallSite), Type.getType(MethodHandles.Lookup), Type.getType(String), Type.getType(MethodType), Type.getType(Class), Type.getType(int.class)),
                                                            false
                                                    ),
                                                    Type.getObjectType(BytecodeHelper.getClassInternalName(field.declaringClass)),
                                                    methodType
                                            )
                                            methodVisitor.visitInsn(Opcodes.ACONST_NULL)
                                        }
                                    })
                            )
                            methodNode.declaringClass.addMethod(bridgeMethod)
                        }
                        MethodNode bridgeMethod = methodNode.declaringClass.getMethods(bridgeMethodName)[0]

                        List<Expression> outParameters = new ArrayList<>()
                        if (!field.static) {
                            outParameters.add((Expression) lhs.objectExpression)
                        }
                        outParameters.add(expr.rightExpression)
                        var classExpression = new ClassExpression(methodNode.declaringClass)
                        classExpression.setNodeMetaData(StaticTypesMarker.INFERRED_TYPE, ClassHelper.makeWithoutCaching(Class).tap {
                            it.setGenericsTypes(new GenericsType[]{new GenericsType(methodNode.declaringClass)})
                        })
                        var out = new MethodCallExpression(
                                transform(classExpression),
                                bridgeMethodName,
                                transform(new ArgumentListExpression(
                                        outParameters
                                ))
                        )

                        out.setNodeMetaData(StaticTypesMarker.DIRECT_METHOD_CALL_TARGET, bridgeMethod)
                        out.type = field.type
                        out.setMethodTarget(bridgeMethod)

                        return out
                    }
                }
                return super.transform(expr)
            }

            private Expression attemptTransformMethod(MethodNode method, Expression expr) {
                if (expr !instanceof MethodCall) {
                    return null
                }
                if (!isAccessible(method.declaringClass, methodNode.declaringClass, method.protected, method.public, method.private) && openedClasses.contains(method.declaringClass)) {
                    boolean isCtor = method.name == OpenClassTypeCheckingExtension.CTOR_DUMMY
                    if (isCtor) {
                        var ctor = method.declaringClass.getDeclaredConstructor(method.parameters)
                        if (ctor !== null && isAccessible(ctor.declaringClass, methodNode.declaringClass, ctor.protected, ctor.public, ctor.private)) {
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
                            return transform(out)
                        }
                    }
                    String outMethodPart = "\$\$${method.declaringClass.name.replace('.', '$')}\$\$${method.name}\$\$${method.parameters.collect { it.type.name.replace('.', '$') }.join('$')}"
                    String bridgeMethodName = "\$dev_lukebemish_opensesame_bridge${outMethodPart}"
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
                    if (methodNode.declaringClass.getMethods(bridgeMethodName).empty) {
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
                                        var methodType = Open.Type.VIRTUAL.ordinal()
                                        if (method.static)
                                            methodType = Open.Type.STATIC.ordinal()
                                        if (!method.static && method.private)
                                            methodType = Open.Type.SPECIAL.ordinal()
                                        if (isCtor)
                                            methodType = Open.Type.CONSTRUCT.ordinal()
                                        methodVisitor.visitInvokeDynamicInsn(
                                                method.name,
                                                BytecodeHelper.getMethodDescriptor(method.returnType, parameters),
                                                new Handle(
                                                        Opcodes.H_INVOKESTATIC,
                                                        BytecodeHelper.getClassInternalName(OPENING_METAFACTORY),
                                                        'invoke',
                                                        Type.getMethodDescriptor(Type.getType(CallSite), Type.getType(MethodHandles.Lookup), Type.getType(String), Type.getType(MethodType), Type.getType(Class), Type.getType(int.class)),
                                                        false
                                                ),
                                                Type.getObjectType(BytecodeHelper.getClassInternalName(method.declaringClass)),
                                                methodType
                                        )
                                        if (method.returnType == ClassHelper.VOID_TYPE) {
                                            methodVisitor.visitInsn(Opcodes.ACONST_NULL)
                                        }
                                    }
                                })
                        )
                        methodNode.declaringClass.addMethod(bridgeMethod)
                    }
                    MethodNode bridgeMethod = methodNode.declaringClass.getMethods(bridgeMethodName)[0]

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
                            transform(classExpression),
                            bridgeMethodName,
                            transform(new ArgumentListExpression(
                                    outParameters
                            ))
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
                return null
            }
        }

        trn.visitMethod(methodNode)
    }

    private static boolean isAccessible(ClassNode target, ClassNode source, boolean isProtected, boolean isPublic, boolean isPrivate) {
        if (isPublic) {
            return true
        }
        if (target == source || target.innerClasses.collect().contains(source) || source.innerClasses.collect().contains(target)) {
            return true
        }
        if (isPrivate) {
            return false
        }
        if (isProtected) {
            return source.isDerivedFrom(target) || target.package == source.package
        }
        return target.package == source.package
    }

    @Override
    int priority() {
        return -1
    }
}
