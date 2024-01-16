package dev.lukebemish.opensesame.plugin.loom;

import dev.lukebemish.opensesame.plugin.OpenSesameExtension;
import dev.lukebemish.opensesame.plugin.OpenSesamePlugin;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.api.remapping.TinyRemapperExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Callable;

public class OpenSesamePluginLoom implements Plugin<Project> {
    private void checkForLoom(Project project) {
        var extension = project.getExtensions().findByName("loom");
        if (!(extension instanceof LoomGradleExtensionAPI)) {
            throw new IllegalStateException("OpenSesame - loom integration plugin must be applied after loom");
        }
    }

    public void apply(Project project) {
        project.getPlugins().apply(OpenSesamePlugin.class);
        checkForLoom(project);

        OpenSesameExtension extension = (OpenSesameExtension) project.getExtensions().getByName("opensesame");
        var loomInteropExtension = extension.getExtensions().create("loom", OpenSesameLoomExtension.class);

        LoomGradleExtensionAPI loomExtension = (LoomGradleExtensionAPI) project.getExtensions().getByName("loom");

        ListProperty<Action<OpeningRemapperExtension.OpeningRemapperParameters>> remappingConfiguration = loomInteropExtension.getRemappingConfigurations();

        ObjectFactory objectFactory = project.getObjects();
        //noinspection Convert2Lambda
        Provider<OpeningRemapperExtension.OpeningRemapperParameters> configuredParameters = project.provider(new Callable<>() {
            @Override
            public OpeningRemapperExtension.OpeningRemapperParameters call() {
                var parameters = objectFactory.newInstance(OpeningRemapperExtension.OpeningRemapperParameters.class);
                for (var action : remappingConfiguration.get()) {
                    action.execute(parameters);
                }
                return parameters;
            }
        });

        //noinspection Convert2Lambda
        loomExtension.addRemapperExtension(OpeningRemapperExtension.class, OpeningRemapperExtension.OpeningRemapperParameters.class, new Action<>() {
            @SuppressWarnings("Convert2Lambda")
            @Override
            public void execute(OpeningRemapperExtension.@NotNull OpeningRemapperParameters parameters) {
                parameters.getContextFilters().set(configuredParameters.map(new Transformer<>() {
                    @SuppressWarnings("UnstableApiUsage")
                    @Override
                    public List<Spec<TinyRemapperExtension.Context>> transform(OpeningRemapperExtension.@NotNull OpeningRemapperParameters ps) {
                        return ps.getContextFilters().get();
                    }
                }));
            }
        });
        loomExtension.getKnownIndyBsms().add("dev/lukebemish/opensesame/runtime/OpeningMetafactory");
    }
}
