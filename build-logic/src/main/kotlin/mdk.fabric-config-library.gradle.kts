plugins {
    id("mdk.config-library")
    id("net.fabricmc.fabric-loom")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraftVersion").toString()}")
    compileOnly("net.fabricmc:fabric-loader:0.19.5")
    compileOnly("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${property("forgeConfigApiPortVersion").toString()}")
}
