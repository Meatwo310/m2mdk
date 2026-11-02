plugins {
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.fabricmc.net/")
    maven("https://nexus.gtnewhorizons.com/repository/public/") {
        content { includeGroupByRegex("com\\.gtnewhorizons(\\..+)?") }
    }
    mavenCentral()
}

dependencies {
    implementation("net.meatwo310.mdk:m2mdk-build-logic:0.1.0-SNAPSHOT")
    implementation(libs.mod.publish.plugin)
    implementation(libs.neoforged.moddev.gradle)
    implementation(libs.forgegradle.plugin)
    implementation(libs.fabric.loom)
    implementation("com.gtnewhorizons.gtnhconvention:com.gtnewhorizons.gtnhconvention.gradle.plugin:2.0.20")
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit"))
}

tasks.processTestResources {
    from("../build-logic/src/main/kotlin") {
        include("mdk.java-library-conventions.gradle.kts", "mdk.config-library.gradle.kts")
        into("resource-composition/conventions")
    }
    from("../build-logic/src/main/kotlin/net/meatwo310/mdk/gradle") {
        into("resource-composition/helpers")
    }
    // Test the current helpers in an isolated build without loading Minecraft plugins.
    from("src/main/kotlin/net/meatwo310/mdk/build") {
        include("SourceSetArtifacts.kt", "ConfigSourceSets.kt", "ConfigCore.kt", "ResourceComposition.kt", "ModInput.kt", "SharedCode.kt")
        into("resource-composition/helpers")
    }
}

tasks.test {
    useJUnit()
    maxParallelForks = 1
    maxHeapSize = "256m"
    testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
}

group = "net.meatwo310.mdk"
version = "0.1.0-SNAPSHOT"

java.withSourcesJar()

publishing {
    repositories.maven {
        name = "local"
        url = uri(providers.gradleProperty("mdk.pluginRepository").orElse(layout.buildDirectory.dir("repository").map { it.asFile.absolutePath }))
    }
}
