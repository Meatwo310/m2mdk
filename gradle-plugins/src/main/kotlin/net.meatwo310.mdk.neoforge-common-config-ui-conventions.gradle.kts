import net.meatwo310.mdk.build.*

requireConfigConvention(
    "net.meatwo310.mdk.neoforge-common-config-ui-conventions",
    "net.meatwo310.mdk.neoforge-common-config-conventions",
)
plugins.withId("java-library") {
    configureVersionCommonConfigUiSourceSet()
}
