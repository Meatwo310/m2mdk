package net.meatwo310.mdk.build

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

fun Project.includeModInputs(vararg inputs: ModInput) {
    configureLocalModInputs()
    for (input in inputs) resourceComposition().include(input)
    val mainSources = extensions.getByType<SourceSetContainer>()
        .named(SourceSet.MAIN_SOURCE_SET_NAME).get().allSource
    tasks.named<Jar>("jar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        for (input in inputs) {
            from(input.output)
        }
    }
    // Common projects do not necessarily create a sources jar. Also support
    // conventions that enable sources jars after including a source set.
    tasks.withType<Jar>().matching { it.name == "sourcesJar" }.configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        // withSourcesJar may add its default sources after this action runs.
        // Always give the project's own sources precedence over shared sources.
        from(mainSources)
        for (input in inputs) {
            from(input.sources)
        }
    }
}

fun Project.localModInput(sourceSet: SourceSet, priority: ResourcePriority): ModInput = ModInput(
    id = "$path:${sourceSet.name}",
    classes = sourceSet.output.classesDirs,
    resources = files(provider { sourceSet.output.resourcesDir }).builtBy(tasks.named(sourceSet.processResourcesTaskName)),
    sources = sourceSet.allSource,
    resourceRoots = files(provider { sourceSet.resources.srcDirs }),
    replacedRuntimeArtifacts = files(),
    priority = priority,
)

/** Source-set names are repository policy, kept outside the composition engine. */
fun Project.configureLocalModInputs() {
    val composition = resourceComposition()
    extensions.getByType<SourceSetContainer>().all {
        if (name == "main" || name == "client") {
            composition.include(localModInput(this, ResourcePriority(0, if (name == "client") 1 else 3)))
        }
        if (name !in setOf("config", "configClient", "mdkDevelopment")) {
            composition.runtime(this, primary = name == "main")
        }
    }
}

fun Project.developmentModSourceSet(): SourceSet {
    configureLocalModInputs()
    return resourceComposition().developmentSourceSet()
}
