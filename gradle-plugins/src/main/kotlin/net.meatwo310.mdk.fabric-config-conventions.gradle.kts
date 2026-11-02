import net.meatwo310.mdk.build.*

val forgeConfigApiPortVersion = project.property("forgeConfigApiPortVersion").toString()

disallowConfigConvention("net.meatwo310.mdk.fabric-legacy-config-conventions", "net.meatwo310.mdk.fabric-config-conventions")
configureFabricConfigConvention(
    forgeConfigApiPortVersion,
    VersionCatalogLibrary.ForgeConfigApiPortFabric,
    VersionConfigSourceSet.REQUIRED,
)
