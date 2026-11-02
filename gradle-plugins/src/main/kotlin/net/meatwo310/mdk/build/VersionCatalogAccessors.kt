package net.meatwo310.mdk.build

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider

enum class VersionCatalogLibrary(val alias: String) {
    FabricApi("fabric-api"),
    FabricLoader("fabric-loader"),
    ForgeConfigApiPortCommon("forge-config-api-port-common"),
    ForgeConfigApiPortCommonNeoForgeApi("forge-config-api-port-common-neoforge-api"),
    ForgeConfigApiPortFabric("forge-config-api-port-fabric"),
    ForgeConfigApiPortFabricLegacy("forge-config-api-port-fabric-legacy"),
    ForgeConfigApiPortForge("forge-config-api-port-forge"),
    Minecraft("minecraft"),
    Mixin("mixin"),
    NightConfigCoreLegacy("night-config-core-legacy"),
    ParchmentData("parchment-data"),
}

enum class VersionCatalogVersion(val alias: String) {
    FabricLoader("fabric-loader"),
    Mixin("mixin"),
}

/** Consumer catalogs may override defaults, but using the plugin does not require one. */
class MdkVersionCatalog(private val project: Project) {
    private val catalog: VersionCatalog?
        get() = project.extensions.findByType(VersionCatalogsExtension::class.java)?.find("libs")?.orElse(null)

    fun library(library: VersionCatalogLibrary): Provider<out ExternalModuleDependency> =
        catalog?.findLibrary(library.alias)?.orElse(null)
            ?: project.provider { project.dependencies.create(defaultLibrary(library)) as ExternalModuleDependency }

    fun module(library: VersionCatalogLibrary): String =
        library(library).get().let { "${it.group}:${it.name}" }

    fun version(version: VersionCatalogVersion): String =
        catalog?.findVersion(version.alias)?.orElse(null)?.requiredVersion ?: when (version) {
            VersionCatalogVersion.FabricLoader -> "0.19.5"
            VersionCatalogVersion.Mixin -> "0.8.7"
        }

    private fun defaultLibrary(library: VersionCatalogLibrary): String = when (library) {
        VersionCatalogLibrary.FabricApi -> "net.fabricmc.fabric-api:fabric-api"
        VersionCatalogLibrary.FabricLoader -> "net.fabricmc:fabric-loader:${version(VersionCatalogVersion.FabricLoader)}"
        VersionCatalogLibrary.ForgeConfigApiPortCommon -> "fuzs.forgeconfigapiport:forgeconfigapiport-common"
        VersionCatalogLibrary.ForgeConfigApiPortCommonNeoForgeApi -> "fuzs.forgeconfigapiport:forgeconfigapiport-common-neoforgeapi"
        VersionCatalogLibrary.ForgeConfigApiPortFabric -> "fuzs.forgeconfigapiport:forgeconfigapiport-fabric"
        VersionCatalogLibrary.ForgeConfigApiPortFabricLegacy -> "net.minecraftforge:forgeconfigapiport-fabric"
        VersionCatalogLibrary.ForgeConfigApiPortForge -> "fuzs.forgeconfigapiport:forgeconfigapiport-forge"
        VersionCatalogLibrary.Minecraft -> "com.mojang:minecraft"
        VersionCatalogLibrary.Mixin -> "org.spongepowered:mixin:${version(VersionCatalogVersion.Mixin)}"
        VersionCatalogLibrary.NightConfigCoreLegacy -> "com.electronwill.night-config:core:3.6.5"
        VersionCatalogLibrary.ParchmentData -> "org.parchmentmc.data:parchment"
    }
}

val Project.versionCatalog: MdkVersionCatalog
    get() = MdkVersionCatalog(this)
