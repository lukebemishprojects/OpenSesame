package dev.lukebemish.opensesame.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TransformWithPriority

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
class OpenSesameBeforeCheckingTransformation extends AbstractASTTransformation implements TransformWithPriority {
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
                if (expr instanceof ConstructorCallExpression && openedClasses.contains(expr.type)) {
                    var out = new MethodCallExpression(
                            new ClassExpression(expr.type),
                            '$opensesame$$new',
                            expr.arguments
                    )
                    return out
                }
                return super.transform(expr)
            }
        }

        trn.visitMethod(methodNode)
    }

    @Override
    int priority() {
        return 1
    }
}
