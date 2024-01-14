package dev.lukebemish.opensesame.plugin.loom;

import dev.lukebemish.opensesame.compile.asm.VisitingOpenProcessor;
import net.fabricmc.loom.api.remapping.RemapperContext;
import net.fabricmc.loom.api.remapping.RemapperExtension;
import net.fabricmc.loom.api.remapping.RemapperParameters;
import net.fabricmc.loom.api.remapping.TinyRemapperExtension;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public abstract class OpeningRemapperExtension implements RemapperExtension<RemapperParameters.None>, TinyRemapperExtension {
    @Nullable
    @Override
    public TinyRemapper.ApplyVisitorProvider getPreApplyVisitor() {
        return (cls, classVisitor) -> {
            if ("named".equals("")) {
                return classVisitor;
            }

            var remapper = cls.getEnvironment().getRemapper();

            return new VisitingOpenProcessor(classVisitor, VisitingOpenProcessor.ANNOTATIONS) {
                @Override
                protected String remapClassName(String name) {
                    return remapper.map(name);
                }

                @Override
                protected String remapMethodName(Type className, String methodName, Type returnType, List<Type> parameters) {
                    return remapper.mapMethodName(
                            className.getInternalName(),
                            methodName,
                            Type.getMethodDescriptor(returnType, parameters.toArray(Type[]::new))
                    );
                }

                @Override
                protected String remapFieldName(Type className, String fieldName, Type fieldType) {
                    return remapper.mapFieldName(
                            className.getInternalName(),
                            fieldName,
                            fieldType.getDescriptor()
                    );
                }
            };
        };
    }

    @Override
    public ClassVisitor insertVisitor(String className, RemapperContext remapperContext, ClassVisitor classVisitor) {
        return classVisitor;
    }
}
