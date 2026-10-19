import net.meatwo310.mdk.build.*


plugins.withId("java-library") {
    val config = configureConfigSourceSet()
    val configClient = configureConfigSourceSet(CONFIG_CLIENT_SOURCE_SET_NAME)
    val sharedConfig = requiredSharedConfigInputs()
    val sharedConfigClient = sharedConfigClientInputs(includeVersion = true)

    config.addConfigClasspath(sharedConfig)
    configClient.addConfigClasspath(sharedConfig, config)
    configClient.addCompileRuntimeClasspathFrom(config)
    for (input in sharedConfigClient) configClient.compileClasspath += input.output

    includeConfigOutput(sharedConfig, config, configClient)
    includeModInputs(*sharedConfigClient.toTypedArray())

    addConfigOutputTo(SourceSet.MAIN_SOURCE_SET_NAME, sharedConfig, config, configClient)
    addConfigInputsTo(SourceSet.MAIN_SOURCE_SET_NAME, *sharedConfigClient.toTypedArray())
}
