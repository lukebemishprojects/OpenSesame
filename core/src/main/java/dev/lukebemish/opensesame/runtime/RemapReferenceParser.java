package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;

@ApiStatus.Internal
class RemapReferenceParser {
    static String findTag(InputStream stream, String tag) throws IOException {
        ClassReader reader = new ClassReader(stream);
        var visitor = new ClassVisitor(Opcodes.ASM9) {
            String found;
            
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                var existing = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals(tag)) {
                    return existing;
                }
                return new MethodVisitor(Opcodes.ASM9, existing) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        found = name;
                        super.visitFieldInsn(opcode, owner, name, descriptor);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        found = name;
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                };
            }
        };
        reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (visitor.found == null) {
            throw new OpeningException("Remap reference method named "+tag+" either not found or contained no method/field instructions");
        }
        return visitor.found;
    }
}
