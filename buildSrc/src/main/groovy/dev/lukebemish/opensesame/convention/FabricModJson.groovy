package dev.lukebemish.opensesame.convention

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

@CompileStatic
abstract class FabricModJson extends DefaultTask {
    @OutputDirectories
    abstract DirectoryProperty getOutputDirectory()

    @Input
    abstract Property<String> getProjectGroup()
    @Input
    abstract Property<String> getProjectName()
    @Input
    abstract Property<String> getProjectVersion()
    @InputFiles
    abstract Property<Configuration> getBundledDependencies()

    @TaskAction
    void generateFabricModJson() {
        String modid = projectGroup.get().replaceAll(/\./, "_")+ "_" + projectName.get()
        Object fmj = [
                'schemaVersion': 1,
                'id': modid,
                'version': projectName.get(),
                'name': projectName.get(),
                'jars': getBundledDependencies().get().files.collect {
                    ['file': "META-INF/jarjar/${it.name}" as String]
                },
        ]
        getOutputDirectory().get().file("fabric.mod.json").asFile.withWriter { writer ->
            writer.write(new JsonBuilder(fmj).toPrettyString())
        }
    }
}
