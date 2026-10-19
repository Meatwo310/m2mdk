package net.meatwo310.mdk.build

import java.io.File
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/** Replace every original copy of a composed resource, including copies in nested CopySpecs. */
private class ComposedResourceFilter(private val directory: File) : Action<FileCopyDetails> {
    override fun execute(details: FileCopyDetails) {
        val composed = directory.resolve(details.path)
        if (composed.isFile && details.file != composed) {
            details.exclude()
        }
    }
}

/** Raw inputs must not survive resource renames or exclusions in sources jars. */
internal class SourceResourceFilter(private val roots: FileCollection) : Action<FileCopyDetails> {
    override fun execute(details: FileCopyDetails) {
        if (roots.files.any { details.file.toPath().startsWith(it.toPath()) }) {
            details.exclude()
        }
    }
}

class ResourceComposition internal constructor(private val project: Project) {
    private val sources = mutableListOf<ModInput>()
    private val resourceDirectory = project.layout.buildDirectory.dir("mdk/composedResources")
    val resources: TaskProvider<Sync> = project.tasks.register<Sync>("composeModResources") {
        description = "Composes resources by project and source set priority."
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        into(resourceDirectory)
    }
    private var development: SourceSet? = null
    private val runtimeSourceSets = linkedSetOf<SourceSet>()
    private var primaryRuntime: SourceSet? = null

    fun runtime(sourceSet: SourceSet, primary: Boolean = false) {
        runtimeSourceSets.add(sourceSet)
        if (primary) {
            require(primaryRuntime == null || primaryRuntime === sourceSet) { "Only one primary runtime is allowed" }
            primaryRuntime = sourceSet
        }
    }

    init {
        val filter = ComposedResourceFilter(resourceDirectory.get().asFile)
        project.tasks.named<Jar>("jar") {
            from(resources)
            eachFile(filter)
        }
        project.tasks.withType<Jar>().matching { it.name == "sourcesJar" }.configureEach {
            from(resources)
            eachFile(filter)
        }
        project.afterEvaluate {
            val ordered = orderedInputs()
            this@ResourceComposition.resources.configure {
                for (input in ordered) {
                    from(input.resources)
                }
            }
            val sourceResourceFilter = SourceResourceFilter(
                project.files(sources.map { it.resourceRoots }),
            )
            project.tasks.withType<Jar>().matching { it.name == "sourcesJar" }.configureEach {
                eachFile(sourceResourceFilter)
            }
            development?.let { configureDevelopmentClasspath(it) }
        }
    }

    private fun orderedInputs(): List<ModInput> = sources.sortedWith(
        compareBy({ it.priority.layer }, { it.priority.withinLayer }),
    )

    fun include(input: ModInput) {
        val existing = sources.find { it.id == input.id }
        require(existing == null || existing.priority == input.priority) {
            "Conflicting resource priorities for '${input.id}'"
        }
        if (existing == null) sources.add(input)
    }

    /** A single mod root for all loaders, without modifying compilation outputs. */
    fun developmentSourceSet(): SourceSet {
        development?.let { return it }
        val sourceSets = project.extensions.getByType<SourceSetContainer>()
        val directory = project.layout.buildDirectory.dir("mdk/developmentMod")
        val stage = project.tasks.register<Sync>("stageModForDevelopment") {
            description = "Stages the complete mod for development runs."
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            into(directory)
            from(resources)
            eachFile(ComposedResourceFilter(resourceDirectory.get().asFile))
        }
        // Resolve the contributors after all optional conventions have registered their outputs.
        project.afterEvaluate {
            stage.configure {
                for (input in sources) {
                    from(input.output)
                }
            }
        }
        return sourceSets.create("mdkDevelopment") {
            java.setSrcDirs(emptyList<String>())
            resources.setSrcDirs(emptyList<String>())
            (output.classesDirs as ConfigurableFileCollection).setFrom(stage)
            output.setResourcesDir(directory.get().asFile)
            output.dir(mapOf("builtBy" to stage), directory)
        }.also { development = it }
    }

    private fun configureDevelopmentClasspath(development: SourceSet) {
        val originalOutputs = project.files(sources.map { it.output })
        val commonJars = project.files(sources.map { it.replacedRuntimeArtifacts })
        // Keep each environment's dependencies (notably Fabric's client/server Minecraft jars),
        // replacing only the mod's own roots and shared project jars with the staged mod.
        for (target in runtimeSourceSets) {
            target.runtimeClasspath = development.output + (target.runtimeClasspath - originalOutputs - commonJars)
        }
        val primary = requireNotNull(primaryRuntime) { "Register a primary runtime before staging a mod" }
        development.runtimeClasspath = primary.runtimeClasspath
        development.compileClasspath = primary.compileClasspath
    }
}

fun Project.resourceComposition(): ResourceComposition =
    extensions.findByType(ResourceComposition::class.java)
        ?: ResourceComposition(this).also { extensions.add("mdkResourceComposition", it) }
