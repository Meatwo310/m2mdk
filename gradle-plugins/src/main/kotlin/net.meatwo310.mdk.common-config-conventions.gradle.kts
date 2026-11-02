import net.meatwo310.mdk.build.configureConfigSourceSet
import net.meatwo310.mdk.build.includeConfigOutput
import net.meatwo310.mdk.build.CONFIG_CLIENT_SOURCE_SET_NAME
import net.meatwo310.mdk.build.addConfigClasspath

plugins.withId("java-library") {
    val config = configureConfigSourceSet()
    includeConfigOutput(config)
    configureConfigSourceSet(CONFIG_CLIENT_SOURCE_SET_NAME).addConfigClasspath(config)
}
