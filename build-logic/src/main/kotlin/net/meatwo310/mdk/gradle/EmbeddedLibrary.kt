package net.meatwo310.mdk.gradle

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

abstract class EmbeddedLibraryArchives {
    @get:Inject
    abstract val archives: ArchiveOperations
}

data class EmbeddedLibrary(
    val classes: org.gradle.api.file.FileCollection,
    val resources: org.gradle.api.file.FileCollection,
    val sources: org.gradle.api.file.FileCollection,
    val binaries: org.gradle.api.file.FileCollection,
)

private class EmbeddedLibraryCache(val inputs: MutableMap<String, EmbeddedLibrary> = linkedMapOf())

/** Consume ordinary Java library variants, also when substituted by a composite build. */
fun Project.embeddedLibrary(notation: String): EmbeddedLibrary {
    val cache = extensions.findByType(EmbeddedLibraryCache::class.java)
        ?: EmbeddedLibraryCache().also { extensions.add("mdkEmbeddedLibraries", it) }
    cache.inputs[notation]?.let { return it }
    val taskSuffix = notation.substringBeforeLast(":").substringAfterLast(":")
        .split("-", ".").joinToString("") { it.replaceFirstChar(Char::uppercase) }
    fun resolve(sources: Boolean): Configuration =
        configurations.detachedConfiguration(dependencies.create(notation)).apply {
            isTransitive = true
            attributes {
                attribute(org.gradle.api.attributes.Attribute.of("net.meatwo310.mdk.embedded", String::class.java), "true")
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
    val archives = objects.newInstance(EmbeddedLibraryArchives::class.java).archives
    // Resolve only when a consuming task needs the files. Keep producer task dependencies.
    val binaryTrees = binaries.elements.map { files -> files.map { archives.zipTree(it.asFile) } }
    val sourceTrees = sources.elements.map { files -> files.map { archives.zipTree(it.asFile) } }
    val classes = tasks.register<Sync>("stage${taskSuffix}Classes") {
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
        from(binaryTrees)
        include("**/*.class")
        into(layout.buildDirectory.dir("mdk/libraries/$taskSuffix/classes"))
    }
    val resources = tasks.register<Sync>("stage${taskSuffix}Resources") {
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
        from(binaryTrees)
        exclude("**/*.class", "META-INF/MANIFEST.MF")
        into(layout.buildDirectory.dir("mdk/libraries/$taskSuffix/resources"))
    }
    val javaSources = tasks.register<Sync>("stage${taskSuffix}Sources") {
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
        from(sourceTrees)
        include("**/*.java")
        into(layout.buildDirectory.dir("mdk/libraries/$taskSuffix/sources"))
    }
    return EmbeddedLibrary(files(classes), files(resources), files(javaSources).asFileTree, binaries)
        .also { cache.inputs[notation] = it }
}
