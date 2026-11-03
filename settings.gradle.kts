pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases")
        gradlePluginPortal()
    }
    includeBuild("build-logic") { name = "m2mdk-build-logic" }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "m2mdk"

includeBuild("gradle-plugins") { name = "m2mdk-gradle-plugins" }

include(":config-core")
project(":config-core").projectDir = file("packages/config-core")

// Selecting Minecraft versions reduces the number of projects Gradle configures.
// Start small; additional versions (or "all") must be selected explicitly.
val minecraftVersions = providers.gradleProperty("mdk.libraryMinecraft")
    .orElse(providers.environmentVariable("MDK_LIBRARY_MINECRAFT"))
    .orElse(providers.gradleProperty("mdk.minecraft"))
    .orElse(providers.environmentVariable("MDK_MINECRAFT"))
    .orElse("26.1.2").get().split(",").map { it.trim() }.toSet()
val availableMinecraftVersions = file("packages").listFiles()!!
    .filter { it.isDirectory && !it.resolve("build.gradle.kts").isFile }
    .map { it.name }.toSet()
require(minecraftVersions.all { it in availableMinecraftVersions || it == "none" || it == "all" }) {
    "Unknown mdk.libraryMinecraft selection: $minecraftVersions. Choose ${availableMinecraftVersions.sorted()}, all, or none."
}
file("packages").listFiles()!!.filter { it.isDirectory }.sortedBy { it.name }.forEach { directory ->
    if (directory.name == "config-core") return@forEach
    if (directory.resolve("build.gradle.kts").isFile) {
        include(":${directory.name}")
        project(":${directory.name}").projectDir = directory
    } else if ("all" in minecraftVersions || directory.name in minecraftVersions) {
        directory.walkTopDown().maxDepth(3).onEnter { it.name !in setOf("build", ".gradle", ".git") }.filter { it.name == "build.gradle.kts" }.sortedBy { it.path }.forEach { build ->
            val name = build.parentFile.relativeTo(file("packages")).invariantSeparatorsPath.replace('/', '-')
            include(":$name")
            project(":$name").projectDir = build.parentFile
        }
    }
}
