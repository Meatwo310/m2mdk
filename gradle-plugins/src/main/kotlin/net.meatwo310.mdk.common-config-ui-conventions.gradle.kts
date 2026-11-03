import net.meatwo310.mdk.build.*

requireConfigConvention("net.meatwo310.mdk.common-config-ui-conventions", "net.meatwo310.mdk.common-config-conventions")
plugins.withId("java-library") {
    val config = extensions.getByType<SourceSetContainer>().getByName(CONFIG_SOURCE_SET_NAME)
    configureConfigSourceSet(CONFIG_CLIENT_SOURCE_SET_NAME).addConfigClasspath(config)
}
