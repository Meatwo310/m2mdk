package net.meatwo310.mdk.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.File
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.register

/** Relocate library classes and their callers together, before loader remapping. */
data class MdkRelocation(val classes: FileCollection, val sources: FileCollection)

/** Replace raw files without excluding the replacement CopySpec as well. */
class RelocatedFileFilter(private val directory: File, private val suffix: String) : Action<FileCopyDetails> {
    override fun execute(details: FileCopyDetails) {
        if (details.path.endsWith(suffix) && !details.file.toPath().startsWith(directory.toPath())) {
            details.exclude()
        }
    }
}

@CacheableTask
abstract class RelocateMdkSources : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputSources: ConfigurableFileCollection

    @get:Input
    abstract val targetPackage: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun relocate() {
        val directory = outputDirectory.get().asFile
        directory.deleteRecursively()
        directory.mkdirs()
        val prefix = Regex("(?<![\\w$])net\\.meatwo310\\.mdk(?=[.$;\\s])")
        val packageDeclaration = Regex("(?m)^package\\s+([\\w.]+)\\s*;")
        val written = mutableSetOf<String>()
        inputSources.asFileTree.matching { include("**/*.java") }.visit {
            if (!isDirectory) {
                val text = prefix.replace(file.readText()) { targetPackage.get() }
                val packageName = packageDeclaration.find(text)?.groupValues?.get(1)
                val path = (packageName?.replace('.', '/')?.plus("/") ?: "") + file.name
                if (written.add(path)) {
                    directory.resolve(path).apply { parentFile.mkdirs(); writeText(text) }
                }
            }
        }
    }
}

fun Project.relocateMdk(
    inputClasses: FileCollection,
    inputSources: FileCollection,
    targetPackage: String,
): MdkRelocation {
    require(targetPackage.matches(Regex("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)+"))) {
        "Invalid embedded library package: $targetPackage"
    }
    require(!targetPackage.startsWith("net.meatwo310.mdk")) {
        "Embedded libraries must use a package owned by the consuming Mod"
    }
    val archives = objects.newInstance(EmbeddedLibraryArchives::class.java).archives
    val archive = tasks.register<ShadowJar>("relocateMdkClasses") {
        description = "Relocates embedded m2mdk classes and all Mod references to them."
        configurations.set(emptyList())
        archiveFileName.set("relocated-classes.jar")
        destinationDirectory.set(layout.buildDirectory.dir("mdk/relocation"))
        from(inputClasses)
        include("**/*.class")
        relocate("net.meatwo310.mdk", targetPackage)
    }
    val classes = tasks.register<Sync>("stageRelocatedMdkClasses") {
        from(archive.map { archives.zipTree(it.archiveFile.get().asFile) })
        include("**/*.class")
        into(layout.buildDirectory.dir("mdk/relocation/classes"))
    }
    val sources = tasks.register<RelocateMdkSources>("relocateMdkSources") {
        this.inputSources.from(inputSources.asFileTree.matching { include("**/*.java") })
        this.targetPackage.set(targetPackage)
        outputDirectory.set(layout.buildDirectory.dir("mdk/relocation/sources"))
    }
    return MdkRelocation(files(classes), files(sources))
}
