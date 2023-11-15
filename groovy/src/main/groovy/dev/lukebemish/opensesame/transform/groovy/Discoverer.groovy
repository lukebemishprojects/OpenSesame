package dev.lukebemish.opensesame.transform.groovy

import com.google.auto.service.AutoService
import dev.lukebemish.opensesame.annotations.Open
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@AutoService(ASTTransformation)
class Discoverer implements ASTTransformation {
    private static final ClassNode OPEN = ClassHelper.makeWithoutCaching(Open)

    private final OpenTransformation openTransformation = new OpenTransformation()

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        List<ClassNode> classes = new ArrayList<>(source.AST.classes)
        for (def classNode : classes) {
            List<MethodNode> methods = new ArrayList<>(classNode.methods)
            for (def method : methods) {
                if (!method.getAnnotations(OPEN).empty) {
                    for (def annotation : method.getAnnotations(OPEN)) {
                        openTransformation.visit(new ASTNode[] {annotation, method}, source)
                    }
                }
            }
        }
    }
}
