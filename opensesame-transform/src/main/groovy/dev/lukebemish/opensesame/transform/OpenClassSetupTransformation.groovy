package dev.lukebemish.opensesame.transform

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OpenClassSetupTransformation extends AbstractASTTransformation {
    static final ClassNode COMPILE_STATIC = ClassHelper.makeWithoutCaching(CompileStatic)
    static final ClassNode COMPILE_DYNAMIC = ClassHelper.makeWithoutCaching(CompileDynamic)
    static final ClassNode TYPE_CHECKED = ClassHelper.makeWithoutCaching(TypeChecked)
    static final String TYPE_CHECKER_PATH = 'dev.lukebemish.opensesame.transform.OpenClassTypeCheckingExtension'

    private static boolean isStaticCompiled(AnnotatedNode node) {
        if (node.getAnnotations(COMPILE_STATIC).size() > 0) {
            return true
        }
        if (node.getAnnotations(COMPILE_DYNAMIC).size() > 0) {
            return false
        }
        if (node.declaringClass !== null && node.declaringClass != node) {
            return isStaticCompiled(node.declaringClass)
        }
        if (node instanceof ClassNode && node.outerClass !== null) {
            return isStaticCompiled(node.outerClass)
        }
        return false
    }

    private void addTypeChecker(AnnotatedNode node) {
        node.getAnnotations(TYPE_CHECKED).each {
            var extensionsMember = it.getMember('extensions')
            if (extensionsMember instanceof ListExpression) {
                extensionsMember.addExpression(new ConstantExpression(TYPE_CHECKER_PATH))
                it.setMember('extensions', extensionsMember)
            } else if (extensionsMember instanceof ConstantExpression) {
                var newExtensionsMember = new ListExpression()
                newExtensionsMember.addExpression(extensionsMember)
                newExtensionsMember.addExpression(new ConstantExpression(TYPE_CHECKER_PATH))
                it.setMember('extensions', newExtensionsMember)
            } else {
                it.setMember('extensions', new ConstantExpression(TYPE_CHECKER_PATH))
            }
        }
        node.getAnnotations(COMPILE_STATIC).each {
            var extensionsMember = it.getMember('extensions')
            if (extensionsMember instanceof ListExpression) {
                if (extensionsMember.expressions.any {it instanceof ConstantExpression && it.value == TYPE_CHECKER_PATH}) {
                    return
                }
                extensionsMember.addExpression(new ConstantExpression(TYPE_CHECKER_PATH))
                it.setMember('extensions', extensionsMember)
            } else if (extensionsMember instanceof ConstantExpression) {
                var newExtensionsMember = new ListExpression()
                if (extensionsMember.value == TYPE_CHECKER_PATH) {
                    return
                }
                newExtensionsMember.addExpression(extensionsMember)
                newExtensionsMember.addExpression(new ConstantExpression(TYPE_CHECKER_PATH))
                it.setMember('extensions', newExtensionsMember)
            } else {
                it.setMember('extensions', new ConstantExpression(TYPE_CHECKER_PATH))
            }
        }
        if (node.declaringClass !== null && node.declaringClass != node) {
            addTypeChecker(node.declaringClass)
        }
        if (node instanceof ClassNode && node.outerClass !== null) {
            addTypeChecker(node.outerClass)
        }
    }

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)

        MethodNode methodNode = (MethodNode) nodes[1]

        if (!isStaticCompiled(methodNode)) {
            throw new RuntimeException('OpenSesame can only be used on methods that are statically compiled')
        }

        addTypeChecker(methodNode)
    }
}
