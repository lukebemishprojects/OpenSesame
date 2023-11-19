package dev.lukebemish.opensesame.plugin.loom;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.Project;

import java.io.IOException;

class LoomUtils {
    static String getMappingsFile(Project project) {
        LoomGradleExtensionAPI loomExtension = (LoomGradleExtensionAPI) project.getExtensions().getByName("loom");
        try {
            return loomExtension.getMappingsFile().getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
