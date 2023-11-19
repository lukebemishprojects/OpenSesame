package dev.lukebemish.opensesame.convention

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry

// Stole my own code from groovybundler... it works though
@CompileStatic
abstract class Bundler extends DefaultTask {
    @Classpath
    abstract Property<Configuration> getBundleConfiguration()

    @OutputDirectories
    abstract DirectoryProperty getOutputDirectory()

    @Input
    abstract Property<String> getModType()

    @Input
    abstract Property<String> getModulePrefix()

    @TaskAction
    void copyAndProcessJars() {
        var jarjarPath = getOutputDirectory().get().dir("META-INF").dir("jarjar")
        jarjarPath.asFile.deleteDir()
        Files.createDirectories(jarjarPath.asFile.toPath())
        List jarJarJars = []
        for (final artifact : getBundleConfiguration().get().resolvedConfiguration.resolvedArtifacts) {
            var resolvedVersion = artifact.moduleVersion
            String group = resolvedVersion.id.group
            String name = resolvedVersion.id.name
            String version = resolvedVersion.id.version
            String modid = group.replaceAll(/\./, "_")+ "_" + name
            Object fmj = [
                    'schemaVersion': 1,
                    'id': modid,
                    'version': version,
                    'name': name,
                    'custom': [
                            'opensesame.bundler:generated': true,
                    ]
            ]
            var outArchive = jarjarPath.file("${name}-${version}.jar")

            var outLicenseDir = getOutputDirectory().get().dir("META-INF").dir("licenses").dir(name)
            outLicenseDir.asFile.deleteDir()

            try (final input = new JarInputStream(artifact.file.newInputStream())
                 final output = new JarOutputStream(outArchive.asFile.newOutputStream())) {
                final manifest = new Manifest(input.getManifest())
                manifest.mainAttributes.putValue('FMLModType', modType.get())
                if (!manifest.mainAttributes.getValue('Automatic-Module-Name')) {
                    manifest.mainAttributes.putValue('Automatic-Module-Name', (modulePrefix.get() + '.' + group + '.' + name).replace('-', '.'))
                }
                output.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME))
                manifest.write(output)
                output.closeEntry()

                ZipEntry entry
                while ((entry = input.nextEntry) !== null) {
                    if (entry.name.endsWith('module-info.class')) continue
                    ZipEntry newEntry = new ZipEntry(entry.name)
                    if (entry.comment !== null) newEntry.setComment(entry.comment)
                    output.putNextEntry(newEntry)
                    byte[] bytes = input.readAllBytes()
                    output.write(bytes)
                    output.closeEntry()

                    if (entry.name.startsWith("META-INF/licenses") && !entry.isDirectory()) {
                        var licensePath = outLicenseDir.asFile.toPath().resolve(entry.name.substring("META-INF/".length()))
                        Files.createDirectories(licensePath.parent)
                        Files.write(licensePath, bytes)
                    } else if (entry.name.startsWith("META-INF/LICENSE") && !entry.isDirectory()) {
                        var licensePath = outLicenseDir.asFile.toPath().resolve(entry.name.substring("META-INF/".length()))
                        Files.createDirectories(licensePath.parent)
                        Files.write(licensePath, bytes)
                    } else if (entry.name.startsWith("META-INF/NOTICE") && !entry.isDirectory()) {
                        var licensePath = outLicenseDir.asFile.toPath().resolve(entry.name.substring("META-INF/".length()))
                        Files.createDirectories(licensePath.parent)
                        Files.write(licensePath, bytes)
                    } else if (entry.name.startsWith("LICENSE") && !entry.isDirectory()) {
                        var licensePath = outLicenseDir.asFile.toPath().resolve(entry.name)
                        Files.deleteIfExists(licensePath)
                        Files.createDirectories(licensePath.parent)
                        Files.write(licensePath, bytes)
                    }
                }

                ZipEntry fmjEntry = new ZipEntry("fabric.mod.json")
                output.putNextEntry(fmjEntry)
                output.write(new JsonBuilder(fmj).toPrettyString().getBytes(StandardCharsets.UTF_8))
                output.closeEntry()
            }

            jarJarJars << [
                    'identifier': ['group':group, 'artifact':name],
                    'version': ['range':"[${version},)", 'artifactVersion':version],
                    'path': "META-INF/jarjar/${name}-${version}.jar",
                    'isObfuscated': false
            ]
        }
        jarjarPath.file('metadata.json').asFile.write(new JsonBuilder([
                'jars': jarJarJars
        ]).toPrettyString(), 'UTF-8')
    }
}
