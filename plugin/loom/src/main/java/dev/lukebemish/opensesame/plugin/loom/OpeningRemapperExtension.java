package dev.lukebemish.opensesame.plugin.loom;

import dev.lukebemish.opensesame.compile.asm.VisitingProcessor;
import net.fabricmc.loom.api.remapping.RemapperContext;
import net.fabricmc.loom.api.remapping.RemapperExtension;
import net.fabricmc.loom.api.remapping.RemapperParameters;
import net.fabricmc.loom.api.remapping.TinyRemapperExtension;
import net.fabricmc.tinyremapper.TinyRemapper;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Nested;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public abstract class OpeningRemapperExtension implements RemapperExtension<OpeningRemapperExtension.OpeningRemapperParameters>, TinyRemapperExtension {
    @Nested
    public abstract Property<OpeningRemapperParameters> getParameters();

    public OpeningRemapperExtension(OpeningRemapperParameters parameters) {
        this.getParameters().set(parameters);
    }
    
    @Inject
    public OpeningRemapperExtension() {}

    public static abstract class OpeningRemapperParameters implements RemapperParameters {
        @Nested
        public abstract ListProperty<Spec<Context>> getContextFilters();

        public void contextFilter(Spec<TinyRemapperExtension.Context> spec) {
            getContextFilters().add(spec);
        }
    }

    @Nullable
    @Override
    public TinyRemapper.@Nullable ApplyVisitorProvider getPreApplyVisitor(Context context) {
        return (cls, classVisitor) -> {
            for (var spec : getParameters().get().getContextFilters().get()) {
                if (!spec.isSatisfiedBy(context)) {
                    return classVisitor;
                }
            }

            var remapper = cls.getEnvironment().getRemapper();

            return new VisitingProcessor(classVisitor, VisitingProcessor.ANNOTATIONS) {
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
