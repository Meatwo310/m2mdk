plugins {
    id("mdk.config-library")
    id("net.fabricmc.fabric-loom-remap")
}

val minecraftVersion = property("minecraftVersion").toString()
dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${property("parchmentMinecraftVersion").toString()}:${property("parchmentMappingsVersion").toString()}@zip")
    })
    modCompileOnly("net.fabricmc:fabric-loader:0.19.5")
    val portVersion = property("forgeConfigApiPortVersion").toString()
    val portModule = if (minecraftVersion in setOf("1.18.2", "1.19.2"))
        "net.minecraftforge:forgeconfigapiport-fabric" else "fuzs.forgeconfigapiport:forgeconfigapiport-fabric"
    modCompileOnly("$portModule:$portVersion")
    if (minecraftVersion in setOf("1.18.2", "1.19.2")) {
        compileOnly("com.electronwill.night-config:core:3.6.5")
    }
}

// Keep named sources distinct from Loom's published intermediary sources.
tasks.named<Jar>("sourcesJar") {
    archiveClassifier.set("dev-sources")
}
tasks.named<AbstractArchiveTask>("remapSourcesJar") {
    archiveClassifier.set("sources")
}
