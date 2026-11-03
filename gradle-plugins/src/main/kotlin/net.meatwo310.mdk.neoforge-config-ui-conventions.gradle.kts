import net.meatwo310.mdk.build.*

requireConfigConvention("net.meatwo310.mdk.neoforge-config-ui-conventions", "net.meatwo310.mdk.neoforge-config-conventions")
plugins.withId("java-library") {
    configurePlatformConfigUiSourceSet(fabric = false)
}
