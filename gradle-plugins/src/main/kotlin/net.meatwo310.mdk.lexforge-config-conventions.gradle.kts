import net.meatwo310.mdk.build.*

val modId = project.property("modId").toString()
val forgeConfigApiPortVersion = project(sharedCode().versionProject.get())
    .property("forgeConfigApiPortVersion")
    .toString()

plugins.withId("java-library") {
    val config = configureConfigSourceSet()
    val sharedConfig = requiredSharedConfigInputs()

    config.addConfigClasspath(sharedConfig)
    includeConfigOutput(sharedConfig, config)
    val main = extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME).get()
    for (configSourceSet in sharedConfig.including(project, config)) {
        main.compileClasspath += configSourceSet.output
    }
}

plugins.withId("net.minecraftforge.gradle") {
    platformArtifacts {
        publishingDependencies {
            required("forge-config-api-port")
        }
    }
    val dependency = "${versionCatalog.module(VersionCatalogLibrary.ForgeConfigApiPortForge)}:$forgeConfigApiPortVersion"
    dependencies.add("implementation", dependency) {
        isTransitive = false
    }
    dependencies.add("ciRuntimeMods", dependency)

    tasks.named<GenerateLexForgeModMetadata>("generateModMetadata") {
        additionalModsToml.set(
            """

            [[dependencies."$modId"]]
            modId="forgeconfigapiport"
            mandatory=true
            versionRange="[$forgeConfigApiPortVersion,)"
            ordering="NONE"
            side="BOTH"
            """.trimIndent() + "\n"
        )
    }
}
