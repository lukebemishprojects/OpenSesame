package dev.lukebemish.opensesame.plugin.loom;

import dev.lukebemish.opensesame.plugin.OpenSesameExtension;
import dev.lukebemish.opensesame.plugin.OpenSesamePlugin;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;

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

        loomExtension.addRemapperExtension(OpeningRemapperExtension.class, OpeningRemapperExtension.OpeningRemapperParameters.class, parameters -> {
            for (var action : remappingConfiguration.get()) {
                action.execute(parameters);
            }
        });
        loomExtension.getKnownIndyBsms().add("dev/lukebemish/opensesame/runtime/OpeningMetafactory");
    }
}
