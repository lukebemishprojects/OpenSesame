package dev.lukebemish.opensesame.plugin.loom;

import net.fabricmc.loom.api.remapping.TinyRemapperExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.specs.Spec;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public abstract class OpenSesameLoomExtension {
    public abstract ListProperty<Action<OpeningRemapperExtension.OpeningRemapperParameters>> getRemappingConfigurations();

    @Inject
    public OpenSesameLoomExtension(Project project) {
        //noinspection Convert2Lambda
        getRemappingConfigurations().add(project.provider(() -> new Action<OpeningRemapperExtension.OpeningRemapperParameters>() {
            @Override
            public void execute(OpeningRemapperExtension.@NotNull OpeningRemapperParameters parameters) {
                //noinspection Convert2Lambda
                parameters.getContextFilters().add(new Spec<>() {
                    @SuppressWarnings("UnstableApiUsage")
                    @Override
                    public boolean isSatisfiedBy(TinyRemapperExtension.Context context) {
                        return !context.targetNamespace().equals("named");
                    }
                });
            }
        }));
        getRemappingConfigurations().finalizeValueOnRead();
    }

    public void remappingConfiguration(Action<OpeningRemapperExtension.OpeningRemapperParameters> action) {
        getRemappingConfigurations().add(action);
    }
}
