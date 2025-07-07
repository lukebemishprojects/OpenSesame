package dev.lukebemish.opensesame.convention

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

// Stole my own code from groovybundler... it works though
@CompileStatic
abstract class Bundler extends DefaultTask {
    @Input
    abstract ListProperty<ModuleVersionIdentifier> getBundleArtifactIds()
    @InputFiles
    abstract ListProperty<File> getBundleArtifactFiles()

    @OutputDirectories
    abstract DirectoryProperty getOutputDirectory()

    @Input
    abstract Property<String> getModType()

    @Input
    abstract Property<String> getModulePrefix()

    void bundleConfiguration(Configuration configuration) {
        var artifacts = providers.provider { configuration.resolvedConfiguration.resolvedArtifacts.collect() }
        bundleArtifactIds.set(artifacts.map { it.collect { it.moduleVersion.id} })
        bundleArtifactFiles.set(artifacts.map { it.collect { it.file} })
        dependsOn(configuration)
    }

    private final ProviderFactory providers

    @Inject
    Bundler(ProviderFactory providers) {
        this.providers = providers
    }

    @TaskAction
    void copyAndProcessJars() {
        var jarjarPath = getOutputDirectory().get().dir("META-INF").dir("jarjar")
        jarjarPath.asFile.deleteDir()
        Files.createDirectories(jarjarPath.asFile.toPath())
        List jarJarJars = []

        List<File> artifactFiles = getBundleArtifactFiles().get()
        List<ModuleVersionIdentifier> artifactIds = getBundleArtifactIds().get()

        if (artifactFiles.size() != artifactIds.size()) {
            throw new RuntimeException("Artifact files and artifact ids are not the same size")
        }

        for (int i = 0; i < artifactFiles.size(); i++) {
            File artifactFile = artifactFiles.get(i)
            ModuleVersionIdentifier artifactId = artifactIds.get(i)
            ModuleIdentifier resolvedVersion = artifactId.module
            String group = resolvedVersion.group
            String name = resolvedVersion.name
            String version = artifactId.version
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

            var hasModsToml = false
            try {
                var modsToml = new ZipFile(artifactFile).getEntry("META-INF/mods.toml")
                if (modsToml !== null) {
                    hasModsToml = true
                }
            } catch (Exception ignored) {}

            try (final input = new JarInputStream(artifactFile.newInputStream())
                 final output = new JarOutputStream(outArchive.asFile.newOutputStream())) {
                final manifest = new Manifest(input.getManifest())
                // only add attribute it it doesn't exist and doesn't have a mods.toml file
                if (!manifest.mainAttributes.getValue('FMLModType') && !hasModsToml) {
                    manifest.mainAttributes.putValue('FMLModType', modType.get())
                }
                if (!manifest.mainAttributes.getValue('Automatic-Module-Name')) {
                    manifest.mainAttributes.putValue('Automatic-Module-Name', (modulePrefix.get() + '.' + group + '.' + name).replace('-', '.'))
                }
                output.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME))
                manifest.write(output)
                output.closeEntry()

                boolean hasFmj = false

                ZipEntry entry
                while ((entry = input.nextEntry) !== null) {
                    if (entry.name.endsWith('module-info.class')) continue
                    if (entry.name.endsWith('fabric.mod.json')) {
                        hasFmj = true
                    }
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

                if (!hasFmj) {
                    ZipEntry fmjEntry = new ZipEntry("fabric.mod.json")
                    output.putNextEntry(fmjEntry)
                    output.write(new JsonBuilder(fmj).toPrettyString().getBytes(StandardCharsets.UTF_8))
                    output.closeEntry()
                }
            }

            jarJarJars << [
                    'identifier': ['group':group, 'artifactId':name],
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
