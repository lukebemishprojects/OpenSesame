package dev.lukebemish.opensesame.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.InvokeInstruction;

@ApiStatus.Internal
class RemapReferenceParser {
    static String findTag(InputStream stream, String tag) throws IOException {
        var classFile = ClassFile.of().parse(stream.readAllBytes());
        String found = null;
        for (var method : classFile.methods()) {
            if (method.methodName().equalsString(tag)) {
                if (method.code().isPresent()) {
                    for (var insn : method.code().get().elementList()) {
                        if (insn instanceof FieldInstruction fieldInsn) {
                            found = fieldInsn.name().stringValue();
                        } else if (insn instanceof InvokeInstruction invokeInsn) {
                            found = invokeInsn.name().stringValue();
                        }
                    }
                }
            }
        }
        if (found == null) {
            throw new OpeningException("Remap reference method named "+tag+" either not found or contained no method/field instructions");
        }
        return found;
    }
}
