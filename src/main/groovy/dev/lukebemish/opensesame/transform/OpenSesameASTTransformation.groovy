package dev.lukebemish.opensesame.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.jetbrains.annotations.Nullable

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OpenSesameASTTransformation extends AbstractASTTransformation {
    static final ClassNode COMPILE_STATIC = ClassHelper.makeWithoutCaching(CompileStatic)
    static final String TYPE_CHECKER_PATH = 'dev.lukebemish.opensesame.transform.OpenedTypeCheckingExtension'

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)

        MethodNode methodNode = (MethodNode) nodes[1]

        if (methodNode.getAnnotations(COMPILE_STATIC).isEmpty()) {
            methodNode.addAnnotation(COMPILE_STATIC)
        }

        AnnotationNode compileStaticNode = methodNode.getAnnotations(COMPILE_STATIC)[0]
        @Nullable extensionsMember = compileStaticNode.getMember('extensions')
        if (extensionsMember instanceof ConstantExpression) {
            var oldExtensionMember = extensionsMember
            extensionsMember = new ListExpression()
            extensionsMember.addExpression(oldExtensionMember)
            extensionsMember.addExpression(new ConstantExpression(TYPE_CHECKER_PATH))
        } else if (extensionsMember instanceof ListExpression) {
            extensionsMember.addExpression(new ConstantExpression(TYPE_CHECKER_PATH))
        } else {
            extensionsMember = new ConstantExpression(TYPE_CHECKER_PATH)
        }
        compileStaticNode.setMember('extensions', extensionsMember)
    }
}
