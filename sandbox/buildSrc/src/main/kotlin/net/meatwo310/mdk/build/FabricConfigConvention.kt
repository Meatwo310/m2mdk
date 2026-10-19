package net.meatwo310.mdk.build

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

enum class VersionConfigSourceSet {
    REQUIRED,
    OPTIONAL,
}

fun Project.configureFabricConfigConvention(
    forgeConfigApiPortVersion: String,
    dependencyLibrary: VersionCatalogLibrary,
    versionConfigSourceSet: VersionConfigSourceSet,
) {
    plugins.withId("fabric-mod-conventions") {
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
        val configClient = configureConfigSourceSet(CONFIG_CLIENT_SOURCE_SET_NAME)
        val sourceSets = extensions.getByType<SourceSetContainer>()
        val sharedConfig = when (versionConfigSourceSet) {
            VersionConfigSourceSet.REQUIRED -> requiredSharedConfigInputs()
            VersionConfigSourceSet.OPTIONAL ->
                optionalSharedConfigInputs()
        }
        val client = sourceSets.findByName("client")
        val sharedConfigClient = sharedConfigClientInputs(includeVersion = sharedConfig.version != null)

        config.addConfigClasspath(sharedConfig)
        configClient.addConfigClasspath(sharedConfig, config)
        for (input in sharedConfigClient) configClient.compileClasspath += input.output
        if (client != null) {
            configClient.addCompileRuntimeClasspathFrom(client)
        }

        includeConfigOutput(sharedConfig, config, configClient)
        includeModInputs(*sharedConfigClient.toTypedArray())

        addConfigOutputTo(SourceSet.MAIN_SOURCE_SET_NAME, sharedConfig, config)
        addConfigOutputTo("client", sharedConfig, config, configClient)
        addConfigInputsTo("client", *sharedConfigClient.toTypedArray())

        dependencies.add("runtimeOnly", files(config.output))
        dependencies.add("runtimeOnly", files(configClient.output))
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
