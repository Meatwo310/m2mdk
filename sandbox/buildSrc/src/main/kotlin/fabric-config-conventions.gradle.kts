import net.meatwo310.mdk.build.*

val forgeConfigApiPortVersion = project.property("forgeConfigApiPortVersion").toString()

disallowConfigConvention("fabric-legacy-config-conventions", "fabric-config-conventions")
configureFabricConfigConvention(
    forgeConfigApiPortVersion,
    VersionCatalogLibrary.ForgeConfigApiPortFabric,
    VersionConfigSourceSet.REQUIRED,
)
