plugins {
    id("mdk.config-library")
    id("net.neoforged.moddev")
}

neoForge {
    version = property("neoVersion").toString()
    val parchmentVersion = findProperty("parchmentMappingsVersion")?.toString()
    if (parchmentVersion != null) {
        parchment {
            minecraftVersion = property("parchmentMinecraftVersion").toString()
            mappingsVersion = parchmentVersion
        }
    }
}
