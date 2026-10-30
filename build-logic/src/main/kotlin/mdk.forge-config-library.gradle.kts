plugins {
    id("mdk.config-library")
}

val minecraftVersion = property("minecraftVersion").toString()
val forgeVersion = "$minecraftVersion-${property("forgeVersion")}"

dependencies {
    // Config registration and Spec binding need only Forge/FML APIs, not a mapped Minecraft JAR.
    compileOnly("net.minecraftforge:forge:$forgeVersion:universal")
    compileOnly("net.minecraftforge:fmlcore:$forgeVersion")
    compileOnly("net.minecraftforge:javafmllanguage:$forgeVersion")
    if (minecraftVersion in setOf("1.18.2", "1.19.2", "1.20.1")) {
        compileOnly("com.electronwill.night-config:core:3.6.5")
    } else {
        compileOnly("fuzs.forgeconfigapiport:forgeconfigapiport-forge:${property("forgeConfigApiPortVersion")}") {
            isTransitive = false
        }
    }
}
