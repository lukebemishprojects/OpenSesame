package dev.lukebemish.opensesame.plugin.loom;

import dev.lukebemish.opensesame.compile.asm.VisitingOpenProcessor;
import net.fabricmc.loom.api.remapping.RemapperContext;
import net.fabricmc.loom.api.remapping.RemapperExtension;
import net.fabricmc.loom.api.remapping.RemapperParameters;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import java.util.List;

public abstract class OpeningRemapperExtension implements RemapperExtension<RemapperParameters.None> {
    @Override
    public ClassVisitor insertVisitor(String className, RemapperContext remapperContext, ClassVisitor classVisitor) {
        if (remapperContext.targetNamespace().equals("named")) {
            return classVisitor;
        }

        return new VisitingOpenProcessor(classVisitor, VisitingOpenProcessor.ANNOTATIONS) {
            @Override
            protected String remapClassName(String name) {
                return remapperContext.remapper().map(name);
            }

            @Override
            protected String remapMethodName(Type className, String methodName, Type returnType, List<Type> parameters) {
                return remapperContext.remapper().mapMethodName(className.getInternalName(), methodName, Type.getMethodDescriptor(returnType, parameters.toArray(Type[]::new)));
            }
        };
    }
}
