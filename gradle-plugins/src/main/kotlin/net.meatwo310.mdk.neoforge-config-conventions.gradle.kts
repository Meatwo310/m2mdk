import net.meatwo310.mdk.build.*


plugins.withId("java-library") {
    val config = configureConfigSourceSet()
    val sharedConfig = requiredSharedConfigInputs()

    config.addConfigClasspath(sharedConfig)
    includeConfigOutput(sharedConfig, config)

    addConfigOutputTo(SourceSet.MAIN_SOURCE_SET_NAME, sharedConfig, config)
}
