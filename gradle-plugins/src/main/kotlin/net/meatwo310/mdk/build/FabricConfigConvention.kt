package net.meatwo310.mdk.build

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.configure

enum class VersionConfigSourceSet {
    REQUIRED,
    OPTIONAL,
}

fun Project.configureFabricConfigConvention(
    forgeConfigApiPortVersion: String,
    dependencyLibrary: VersionCatalogLibrary,
    versionConfigSourceSet: VersionConfigSourceSet,
) {
    plugins.withId("net.meatwo310.mdk.fabric-mod-conventions") {
        platformArtifacts {
            publishingDependencies {
                required("forge-config-api-port")
            }
        }
        extensions.configure<FabricModMetadataExtension>("fabricModMetadata") {
            depend("forgeconfigapiport", ">=$forgeConfigApiPortVersion")
        }
    }

    plugins.withId("java-library") {
        val config = configureConfigSourceSet()
        val sharedConfig = when (versionConfigSourceSet) {
            VersionConfigSourceSet.REQUIRED -> requiredSharedConfigInputs()
            VersionConfigSourceSet.OPTIONAL ->
                optionalSharedConfigInputs()
        }
        config.addConfigClasspath(sharedConfig)

        includeConfigOutput(sharedConfig, config)

        addConfigOutputTo(SourceSet.MAIN_SOURCE_SET_NAME, sharedConfig, config)
        addConfigOutputTo("client", sharedConfig, config)

        dependencies.add("runtimeOnly", files(config.output))
    }

    fun addForgeConfigApiPortDependency(configurationName: String) {
        val dependency = "${versionCatalog.module(dependencyLibrary)}:$forgeConfigApiPortVersion"
        dependencies.add(configurationName, dependency)
        dependencies.add("ciRuntimeMods", dependency)
    }

    plugins.withId("net.fabricmc.fabric-loom") {
        addForgeConfigApiPortDependency("implementation")
    }

    plugins.withId("net.fabricmc.fabric-loom-remap") {
        addForgeConfigApiPortDependency("modImplementation")
    }
}

fun Project.disallowConfigConvention(
    disallowedConventionName: String,
    conventionName: String,
) {
    plugins.withId(disallowedConventionName) {
        error("$conventionName cannot be applied together with $disallowedConventionName.")
    }
}
