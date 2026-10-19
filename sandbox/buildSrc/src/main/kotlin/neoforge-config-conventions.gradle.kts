import net.meatwo310.mdk.build.*


plugins.withId("java-library") {
    val config = configureConfigSourceSet()
    val configClient = configureConfigSourceSet(CONFIG_CLIENT_SOURCE_SET_NAME)
    val sharedConfig = requiredSharedConfigInputs()

    config.addConfigClasspath(sharedConfig)
    configClient.addConfigClasspath(sharedConfig, config)
    configClient.addCompileRuntimeClasspathFrom(config)

    includeConfigOutput(sharedConfig, config, configClient)

    addConfigOutputTo(SourceSet.MAIN_SOURCE_SET_NAME, sharedConfig, config, configClient)
}
