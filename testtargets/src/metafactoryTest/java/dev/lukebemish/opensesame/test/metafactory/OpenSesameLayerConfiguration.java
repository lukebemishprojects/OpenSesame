package dev.lukebemish.opensesame.test.metafactory;

import dev.lukebemish.testingutils.framework.modulelayer.LayerConfiguration;

@LayerConfiguration(
        imports = {
                "dev.lukebemish.opensesame.annotations.*",
                "dev.lukebemish.opensesame.annotations.extend.*"
        },
        requires = "dev.lukebemish.opensesame.core",
        compilerArgs = "-Xplugin:dev.lukebemish.javac-post-processor dev.lukebemish.opensesame"
)
public interface OpenSesameLayerConfiguration {
}
