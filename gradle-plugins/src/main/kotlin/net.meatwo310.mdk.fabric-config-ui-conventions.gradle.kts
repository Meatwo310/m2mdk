import net.meatwo310.mdk.build.*

requireConfigConvention(
    "net.meatwo310.mdk.fabric-config-ui-conventions",
    "net.meatwo310.mdk.fabric-config-conventions",
    "net.meatwo310.mdk.fabric-legacy-config-conventions",
)
plugins.withId("java-library") {
    configurePlatformConfigUiSourceSet(fabric = true)
}
