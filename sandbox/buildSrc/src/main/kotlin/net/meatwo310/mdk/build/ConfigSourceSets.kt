package net.meatwo310.mdk.build

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.getByType

const val CONFIG_SOURCE_SET_NAME = "config"
const val CONFIG_CLIENT_SOURCE_SET_NAME = "configClient"

data class SharedConfigInputs(
    val common: ModInput,
    val version: ModInput?,
) {
    val all: List<ModInput> = listOfNotNull(version, common)

    fun including(project: Project, vararg sourceSets: SourceSet): Array<ModInput> =
        (sourceSets.map { project.localConfigInput(it) } + all).toTypedArray()
}

fun Project.configureConfigSourceSet(name: String = CONFIG_SOURCE_SET_NAME): SourceSet {
    val sourceSets = extensions.getByType<SourceSetContainer>()
    val sourceSet = sourceSets.findByName(name) ?: sourceSets.create(name)
    dependencies.add(sourceSet.implementationConfigurationName, sharedCode().configCoreDependency.get())
    configCoreInput()
    sourceSet.compileClasspath += configurations.getByName("compileClasspath")
    sourceSet.runtimeClasspath += sourceSet.output + sourceSet.compileClasspath
    return sourceSet
}

fun Project.includeConfigOutput(vararg sourceSets: SourceSet) {
    includeModInputs(*sourceSets.map { localConfigInput(it) }.toTypedArray())
}

fun Project.includeConfigOutput(sharedConfig: SharedConfigInputs, vararg sourceSets: SourceSet) {
    includeModInputs(*sharedConfig.including(this, *sourceSets))
}

fun Project.addConfigOutputTo(sourceSetName: String, vararg configSourceSets: SourceSet) {
    val sourceSets = extensions.getByType<SourceSetContainer>()
    sourceSets.findByName(sourceSetName)?.let { target ->
        for (configSourceSet in configSourceSets) {
            target.compileClasspath += configSourceSet.output
            target.runtimeClasspath += configSourceSet.output
            target.output.dir(configSourceSet.output)
            // Keep sources in the same order as the outputs merged into this
            // source set, without adding them to its Java compilation inputs.
            target.allSource.source(configSourceSet.allSource)
        }
    }
}

fun Project.addConfigOutputTo(
    sourceSetName: String,
    sharedConfig: SharedConfigInputs,
    vararg configSourceSets: SourceSet,
) {
    addConfigInputsTo(sourceSetName, *sharedConfig.including(this, *configSourceSets))
}

fun Project.localConfigInput(sourceSet: SourceSet): ModInput =
    localModInput(sourceSet, ResourcePriority(0, if (sourceSet.name == CONFIG_CLIENT_SOURCE_SET_NAME) 0 else 2))

fun Project.requiredSharedConfigInputs(): SharedConfigInputs =
    sharedConfigInputs(includeVersion = true)

fun Project.optionalSharedConfigInputs(): SharedConfigInputs =
    sharedConfigInputs(includeVersion = sharedCode().versionConfigEnabled.get())

private fun Project.sharedConfigInputs(includeVersion: Boolean): SharedConfigInputs {
    val wiring = sharedCode()
    return SharedConfigInputs(
        sharedModInput(wiring.commonProject.get(), CONFIG_SOURCE_SET_NAME, ResourcePriority(2, 2)),
        if (includeVersion) sharedModInput(wiring.versionProject.get(), CONFIG_SOURCE_SET_NAME, ResourcePriority(1, 2)) else null,
    )
}

fun Project.configureVersionCommonConfigSourceSet(): SourceSet {
    val config = configureConfigSourceSet()
    val common = sharedModInput(sharedCode().commonProject.get(), CONFIG_SOURCE_SET_NAME, ResourcePriority(2, 2))
    config.compileClasspath += common.output
    config.runtimeClasspath += common.output
    includeConfigOutput(config)
    configureConfigSourceSet(CONFIG_CLIENT_SOURCE_SET_NAME).apply {
        addConfigClasspath(config)
        addCompileRuntimeClasspathFrom(config)
    }
    return config
}

fun Project.sharedConfigClientInputs(includeVersion: Boolean): List<ModInput> {
    val wiring = sharedCode()
    return listOfNotNull(
        sharedModInput(wiring.commonProject.get(), CONFIG_CLIENT_SOURCE_SET_NAME, ResourcePriority(2, 0)),
        if (includeVersion) sharedModInput(wiring.versionProject.get(), CONFIG_CLIENT_SOURCE_SET_NAME, ResourcePriority(1, 0)) else null,
    )
}

fun Project.addConfigInputsTo(sourceSetName: String, vararg inputs: ModInput) {
    extensions.getByType<SourceSetContainer>().findByName(sourceSetName)?.let { target ->
        for (input in inputs) {
            target.compileClasspath += input.output
            target.runtimeClasspath += input.output
            target.output.dir(input.output)
        }
    }
}

fun SourceSet.addConfigClasspath(vararg sourceSets: SourceSet) {
    for (sourceSet in sourceSets) {
        compileClasspath += sourceSet.output
        runtimeClasspath += sourceSet.output
    }
}

fun SourceSet.addConfigClasspath(sharedConfig: SharedConfigInputs, vararg sourceSets: SourceSet) {
    addConfigClasspath(*sourceSets)
    for (input in sharedConfig.all) {
        compileClasspath += input.output
        runtimeClasspath += input.output
    }
}

fun SourceSet.addCompileRuntimeClasspathFrom(sourceSet: SourceSet) {
    compileClasspath += sourceSet.compileClasspath
    runtimeClasspath += sourceSet.runtimeClasspath
}
