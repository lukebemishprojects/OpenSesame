package dev.lukebemish.opensesame.compile.asm;

import dev.lukebemish.opensesame.compile.Processor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public record ASMClassAccumulator(ClassVisitor classVisitor) implements Processor.ClassAccumulator {
    public record ASMMethodMaker(MethodVisitor methodVisitor) implements Processor.MethodMaker {
        @Override
        public void visitEnd() {
            methodVisitor.visitEnd();
        }

        @Override
        public void visitCode() {
            methodVisitor.visitCode();
        }

        @Override
        public void visitInsn(int opcode) {
            methodVisitor.visitInsn(opcode);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            methodVisitor.visitVarInsn(opcode, var);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            methodVisitor.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            methodVisitor.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitLdcInsn(Object value) {
            methodVisitor.visitLdcInsn(value);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            methodVisitor.visitMaxs(maxStack, maxLocals);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            methodVisitor.visitTypeInsn(opcode, type);
        }
    }

    public record ASMFieldMaker(FieldVisitor fieldVisitor) implements Processor.FieldMaker {
        @Override
        public void visitEnd() {
            fieldVisitor.visitEnd();
        }
    }

    @Override
    public Processor.FieldMaker visitField(int access, String name, String descriptor, String signature, Object value) {
        var fieldVisitor = classVisitor.visitField(access, name, descriptor, signature, value);
        return new ASMFieldMaker(fieldVisitor);
    }

    @Override
    public Processor.MethodMaker visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        var methodVisitor = classVisitor.visitMethod(access, name, descriptor, signature, exceptions);
        return new ASMMethodMaker(methodVisitor);
    }
}
