package net.meatwo310.mdk.build

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/** Repository wiring supplies these paths before convention plugins are applied. */
abstract class SharedCodeExtension {
    abstract val configCoreDependency: Property<String>
    abstract val commonProject: Property<String>
    abstract val versionProject: Property<String>
    abstract val versionConfigEnabled: Property<Boolean>

    init {
        versionConfigEnabled.convention(true)
    }
}

fun Project.sharedCode(configure: SharedCodeExtension.() -> Unit = {}): SharedCodeExtension =
    (extensions.findByType(SharedCodeExtension::class.java)
        ?: extensions.create("sharedCode", SharedCodeExtension::class.java)).apply(configure)

private fun artifactConfiguration(sourceSet: String, part: String): String =
    "mdk${sourceSet.replaceFirstChar { it.uppercase() }}$part"

/** Publish directory artifacts with their producing tasks; consumers never inspect SourceSets. */
fun Project.exportModInputs() {
    extensions.getByType<SourceSetContainer>().all {
        val sourceSet = this
        val parts = listOf("Classes", "Resources", "Sources", "ResourceRoots")
        val outgoing = parts.associateWith { part ->
            configurations.create(artifactConfiguration(name, part)) {
                isCanBeConsumed = true
                isCanBeResolved = false
            }
        }
        val sourcesDirectory = layout.buildDirectory.dir("mdk/sharedSources/${sourceSet.name}")
        val stageSources = tasks.register<Sync>("stage${sourceSet.name.replaceFirstChar { it.uppercase() }}ModSources") {
            description = "Stages filtered source files for shared mod consumers."
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            into(sourcesDirectory)
            from(sourceSet.allSource)
            eachFile(SourceResourceFilter(files(provider { sourceSet.resources.srcDirs })))
        }
        artifacts.add(outgoing.getValue("Sources").name, sourcesDirectory) { builtBy(stageSources) }
        // Capture roots after generated resource directories and optional conventions are registered.
        afterEvaluate {
            fun publish(part: String, files: Iterable<Any>, producer: Any? = null) {
                for (file in files) {
                    artifacts.add(outgoing.getValue(part).name, file) {
                        if (producer != null) builtBy(producer)
                    }
                }
            }
            publish("Classes", sourceSet.output.classesDirs, tasks.named(sourceSet.classesTaskName))
            sourceSet.output.resourcesDir?.let {
                publish("Resources", listOf(it), tasks.named(sourceSet.processResourcesTaskName))
            }
            publish("ResourceRoots", sourceSet.resources.srcDirs)
        }
    }
}

fun Project.sharedModInput(projectPath: String, sourceSet: String, priority: ResourcePriority): ModInput {
    fun resolve(part: String): FileCollection {
        val dependency = dependencies.project(mapOf(
            "path" to projectPath,
            "configuration" to artifactConfiguration(sourceSet, part),
        ))
        return configurations.detachedConfiguration(dependency).apply { isTransitive = false }
    }
    val sourceRoots = resolve("Sources")
    return ModInput(
        id = "$projectPath:$sourceSet",
        classes = resolve("Classes"),
        resources = resolve("Resources"),
        sources = sourceRoots.asFileTree,
        resourceRoots = resolve("ResourceRoots"),
        replacedRuntimeArtifacts = if (sourceSet == SourceSet.MAIN_SOURCE_SET_NAME) {
            configurations.detachedConfiguration(dependencies.project(mapOf("path" to projectPath))).apply {
                isTransitive = false
            }
        } else files(),
        priority = priority,
    )
}

fun Project.includeSharedMainInputs() {
    val wiring = sharedCode()
    includeModInputs(
        sharedModInput(wiring.versionProject.get(), "main", ResourcePriority(1, 3)),
        sharedModInput(wiring.commonProject.get(), "main", ResourcePriority(2, 3)),
    )
}
