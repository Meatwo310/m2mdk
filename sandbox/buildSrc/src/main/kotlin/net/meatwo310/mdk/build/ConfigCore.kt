package net.meatwo310.mdk.build

import net.meatwo310.mdk.gradle.embeddedLibrary
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType

fun Project.configCoreInput(): ModInput = libraryInput(sharedCode().configCoreDependency.get())

fun Project.libraryInput(notation: String): ModInput {
    val library = embeddedLibrary(notation)
    extensions.getByType<SourceSetContainer>().matching { it.name == "main" || it.name == "client" }.all {
        // Mapped libraries publish distribution variants too; compile against the named variant.
        compileClasspath = library.binaries + compileClasspath
    }
    return ModInput(
        id = "library:$notation",
        classes = library.classes,
        resources = library.resources,
        sources = library.sources,
        resourceRoots = files(),
        replacedRuntimeArtifacts = library.binaries,
        priority = ResourcePriority(3, 0),
    ).also { includeModInputs(it) }
}
