package dev.lukebemish.opensesame.compile.groovy

import dev.lukebemish.opensesame.compile.Processor
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TupleConstructor
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovyjarjarasm.asm.MethodVisitor
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.classgen.BytecodeExpression

import java.util.function.Consumer

@CompileStatic
@PackageScope
@TupleConstructor
class GroovyClassAccumulator implements Processor.ClassAccumulator {
    final ClassNode classNode

    @TupleConstructor
    class GroovyFieldMaker implements Processor.FieldMaker {
        final FieldNode fieldNode

        @Override
        void visitEnd() {}
    }

    class GroovyMethodMaker implements Processor.MethodMaker {
        final List<Consumer<MethodVisitor>> visitors = new ArrayList<>()

        private void add(
                @ClosureParams(value = SimpleType, options = "groovyjarjarasm.asm.MethodVisitor")
                Closure visitor
        ) {
            visitors.add(visitor as Consumer<MethodVisitor>)
        }

        @Override
        void visitEnd() {}

        @Override
        void visitCode() {}

        @Override
        void visitInsn(int opcode) {
            add { methodVisitor -> methodVisitor.visitInsn(opcode) }
        }

        @Override
        void visitVarInsn(int opcode, int i) {
            add { methodVisitor -> methodVisitor.visitVarInsn(opcode, i) }
        }

        @Override
        void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            add { methodVisitor -> methodVisitor.visitFieldInsn(opcode, owner, name, descriptor) }
        }

        @Override
        void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            add { methodVisitor -> methodVisitor.visitMethodInsn(opcode, owner, name, descriptor, isInterface) }
        }

        @Override
        void visitLdcInsn(Object value) {
            add { methodVisitor -> methodVisitor.visitLdcInsn(value) }
        }

        @Override
        void visitMaxs(int maxStack, int maxLocals) {
            add { methodVisitor -> methodVisitor.visitMaxs(maxStack, maxLocals) }
        }

        @Override
        void visitTypeInsn(int opcode, String type) {
            add { methodVisitor -> methodVisitor.visitTypeInsn(opcode, type) }
        }
    }

    @Override
    Processor.FieldMaker visitField(int access, String name, String descriptor, String signature, Object value) {
        classNode.addField(name, access, nodeFromType(Type.getType(descriptor)), null)
        return new GroovyFieldMaker(classNode.getField(name))
    }

    @Override
    Processor.MethodMaker visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Type methodType = Type.getMethodType(descriptor)
        Parameter[] parameters = new Parameter[methodType.argumentTypes.length]
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = new Parameter(nodeFromType(methodType.argumentTypes[i]), "arg${i}")
        }
        GroovyMethodMaker methodMaker = new GroovyMethodMaker()
        MethodNode node = new MethodNode(name, access, nodeFromType(methodType.returnType), parameters, new ClassNode[0], new ExpressionStatement(new BytecodeExpression(nodeFromType(methodType.returnType)) {
            @Override
            void visit(MethodVisitor methodVisitor) {
                methodMaker.visitors.each { it.accept(methodVisitor) }
            }
        }))
        classNode.addMethod(node)
        return methodMaker
    }

    private ClassNode nodeFromType(Type type) {
        switch (type.sort) {
            case Type.VOID:
                return ClassHelper.VOID_TYPE
            case Type.BOOLEAN:
                return ClassHelper.boolean_TYPE
            case Type.CHAR:
                return ClassHelper.char_TYPE
            case Type.BYTE:
                return ClassHelper.byte_TYPE
            case Type.SHORT:
                return ClassHelper.short_TYPE
            case Type.INT:
                return ClassHelper.int_TYPE
            case Type.FLOAT:
                return ClassHelper.float_TYPE
            case Type.LONG:
                return ClassHelper.long_TYPE
            case Type.DOUBLE:
                return ClassHelper.double_TYPE
            case Type.ARRAY:
                return nodeFromType(type.elementType).makeArray()
            case Type.OBJECT:
                return ClassHelper.makeWithoutCaching(type.className)
            default:
                throw new RuntimeException("Unknown type: ${type}")
        }
    }
}
