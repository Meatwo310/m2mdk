pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "m2mdk"

include(":config-core")
project(":config-core").projectDir = file("packages/config-core")

// Selecting Minecraft versions reduces the number of projects Gradle configures.
// The default includes every supported version.
val minecraftVersions = providers.gradleProperty("mdk.libraryMinecraft")
    .orElse(providers.environmentVariable("MDK_LIBRARY_MINECRAFT"))
    .orElse(providers.gradleProperty("mdk.minecraft"))
    .orElse(providers.environmentVariable("MDK_MINECRAFT"))
    .orNull?.split(",")?.toSet()
file("packages").listFiles()!!.filter { it.isDirectory }.sortedBy { it.name }.forEach { directory ->
    if (directory.name == "config-core") return@forEach
    if (directory.resolve("build.gradle.kts").isFile) {
        include(":${directory.name}")
        project(":${directory.name}").projectDir = directory
    } else if (minecraftVersions == null || directory.name in minecraftVersions) {
        directory.walkTopDown().maxDepth(3).onEnter { it.name !in setOf("build", ".gradle", ".git") }.filter { it.name == "build.gradle.kts" }.sortedBy { it.path }.forEach { build ->
            val name = build.parentFile.relativeTo(file("packages")).invariantSeparatorsPath.replace('/', '-')
            include(":$name")
            project(":$name").projectDir = build.parentFile
        }
    }
}
