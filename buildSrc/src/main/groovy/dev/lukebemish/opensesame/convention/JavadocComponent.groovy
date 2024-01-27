package dev.lukebemish.opensesame.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.SoftwareComponentFactory

import javax.inject.Inject

class JavadocComponent implements Plugin<Project> {
    static class Extension {
        private final SoftwareComponentFactory softwareComponentFactory
        private final Project project

        @Inject
        Extension(Project project, SoftwareComponentFactory softwareComponentFactory1) {
            this.softwareComponentFactory = softwareComponentFactory1
            this.project = project
        }

        void apply(Configuration configuration) {
            def adhocComponent = softwareComponentFactory.adhoc("javadoc")
            project.components.add(adhocComponent)
            adhocComponent.addVariantsFromConfiguration(configuration) {}
        }
    }

    @Override
    void apply(Project project) {
        project.extensions.create("conventionJavadocComponent", Extension)
    }
}
