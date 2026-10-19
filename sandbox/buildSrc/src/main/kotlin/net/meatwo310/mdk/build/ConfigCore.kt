package net.meatwo310.mdk.build

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

abstract class ConfigCoreArchives {
    @get:Inject
    abstract val archives: ArchiveOperations
}

private class ConfigCoreIntegration(val input: ModInput)

/** Consume ordinary Java library variants, also when substituted by a composite build. */
fun Project.configCoreInput(): ModInput {
    extensions.findByType(ConfigCoreIntegration::class.java)?.let { return it.input }
    val notation = sharedCode().configCoreDependency.get()
    extensions.getByType<SourceSetContainer>().matching { it.name == "main" || it.name == "client" }.all {
        dependencies.add(compileOnlyConfigurationName, notation)
    }
    fun resolve(sources: Boolean): Configuration =
        configurations.detachedConfiguration(dependencies.create(notation)).apply {
            isTransitive = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(if (sources) Category.DOCUMENTATION else Category.LIBRARY))
                if (sources) {
                    attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
                } else {
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                }
            }
        }
    val binaries = resolve(false)
    val sources = resolve(true)
    val archives = objects.newInstance(ConfigCoreArchives::class.java).archives
    // Resolve only when a consuming task needs the files. Keep producer task dependencies.
    val binaryTrees = binaries.elements.map { files -> files.map { archives.zipTree(it.asFile) } }
    val sourceTrees = sources.elements.map { files -> files.map { archives.zipTree(it.asFile) } }
    val classes = tasks.register<Sync>("stageConfigCoreClasses") {
        from(binaryTrees)
        include("**/*.class")
        into(layout.buildDirectory.dir("mdk/configCore/classes"))
    }
    val resources = tasks.register<Sync>("stageConfigCoreResources") {
        from(binaryTrees)
        exclude("**/*.class", "META-INF/MANIFEST.MF")
        into(layout.buildDirectory.dir("mdk/configCore/resources"))
    }
    val javaSources = tasks.register<Sync>("stageConfigCoreSources") {
        from(sourceTrees)
        include("**/*.java")
        into(layout.buildDirectory.dir("mdk/configCore/sources"))
    }
    return ModInput(
        id = "library:$notation",
        classes = files(classes),
        resources = files(resources),
        sources = files(javaSources).asFileTree,
        resourceRoots = files(),
        replacedRuntimeArtifacts = binaries,
        priority = ResourcePriority(3, 0),
    ).also {
        extensions.add("mdkConfigCore", ConfigCoreIntegration(it))
        includeModInputs(it)
    }
}
