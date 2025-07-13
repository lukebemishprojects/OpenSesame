package dev.lukebemish.opensesame.plugin.loom;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Nested;

import javax.inject.Inject;

public abstract class OpenSesameLoomExtension {
    @Nested
    public abstract ListProperty<Action<OpeningRemapperExtension.OpeningRemapperParameters>> getRemappingConfigurations();

    @Inject
    public OpenSesameLoomExtension(Project project) {
        //noinspection Convert2Lambda
        getRemappingConfigurations().add(paramters -> paramters.getContextFilters().add(context -> !context.targetNamespace().equals("named")));
        getRemappingConfigurations().finalizeValueOnRead();
    }

    public void remappingConfiguration(Action<OpeningRemapperExtension.OpeningRemapperParameters> action) {
        getRemappingConfigurations().add(action);
    }
}
