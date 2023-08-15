package dev.lukebemish.opensesame.transform

import dev.lukebemish.opensesame.OpenSesame
import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.AttributeExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MethodCall
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor
import org.codehaus.groovy.transform.stc.TypeCheckingExtension

@CompileStatic
class OpenedTypeCheckingExtension extends TypeCheckingExtension {
    private static final ClassNode OPEN_SESAME = ClassHelper.makeWithoutCaching(OpenSesame)

    OpenedTypeCheckingExtension(StaticTypeCheckingVisitor typeCheckingVisitor) {
        super(typeCheckingVisitor)
    }

    private List<Set<ClassNode>> openedClassesList = new ArrayList<>()

    private Set<ClassNode> getClassesToOpen() {
        return openedClassesList.collectMany {it}.toSet()
    }

    @Override
    boolean beforeVisitClass(ClassNode node) {
        Set<ClassNode> openedClasses = new HashSet<>()
        node.getAnnotations(OPEN_SESAME).each {
            var expression = it.getMember('value')
            if (expression instanceof ListExpression) {
                for (final classExpression : expression.expressions) {
                    if (classExpression instanceof ClassExpression) {
                        openedClasses.add(classExpression.getType())
                    }
                }
            } else if (expression instanceof ClassExpression) {
                openedClasses.add(expression.getType())
            }
        }
        openedClassesList.add(openedClasses)
        return super.beforeVisitClass(node)
    }

    @Override
    boolean beforeVisitMethod(MethodNode node) {
        Set<ClassNode> openedClasses = new HashSet<>()
        node.getAnnotations(OPEN_SESAME).each {
            var expression = it.getMember('value')
            if (expression instanceof ListExpression) {
                for (final classExpression : expression.expressions) {
                    if (classExpression instanceof ClassExpression) {
                        openedClasses.add(classExpression.getType())
                    }
                }
            } else if (expression instanceof ClassExpression) {
                openedClasses.add(expression.getType())
            }
        }
        openedClassesList.add(openedClasses)
        return super.beforeVisitMethod(node)
    }

    @Override
    void afterVisitClass(ClassNode node) {
        openedClassesList.remove(openedClassesList.size() - 1)
        super.afterVisitClass(node)
    }

    @Override
    void afterVisitMethod(MethodNode node) {
        openedClassesList.remove(openedClassesList.size() - 1)
        super.afterVisitMethod(node)
    }

    @Override
    List<MethodNode> handleMissingMethod(ClassNode receiver, String name, ArgumentListExpression argumentList, ClassNode[] argumentTypes, MethodCall call) {
        Set<ClassNode> openedClasses = getClassesToOpen()
        if (receiver == classNodeFor(Class)) {
            // try static stuff
            GenericsType type = receiver.genericsTypes[0]
            if (type.upperBounds === null && type.lowerBound === null && type.type !== null) {
                ClassNode staticReceiver = type.type
                if (openedClasses.contains(staticReceiver)) {
                    if (name == '$opensesame$$new') {
                        return staticReceiver.getMethods(name) + (staticReceiver.getDeclaredConstructors().collect {
                            var out = new MethodNode(
                                    '$opensesame$$new',
                                    Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                    staticReceiver,
                                    it.parameters,
                                    it.exceptions,
                                    null
                            )
                            out.setDeclaringClass(staticReceiver)
                            out
                        } as Collection<MethodNode>)
                    }
                    return staticReceiver.getMethods(name)
                }
            }
        }
        if (openedClasses.contains(receiver)) {
            return receiver.getMethods(name)
        }
        return super.handleMissingMethod(receiver, name, argumentList, argumentTypes, call)
    }

    @Override
    boolean handleUnresolvedAttribute(AttributeExpression aexp) {
        return super.handleUnresolvedAttribute(aexp)
    }
}
