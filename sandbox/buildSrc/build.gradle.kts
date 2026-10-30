plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.fabricmc.net/")
    mavenCentral()
}

dependencies {
    implementation("net.meatwo310.mdk:m2mdk-build-logic:0.1.0-SNAPSHOT")
    implementation(libs.mod.publish.plugin)
    implementation(libs.neoforged.moddev.gradle)
    implementation(libs.forgegradle.plugin)
    implementation(libs.fabric.loom)
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit"))
}

tasks.processTestResources {
    from("../../build-logic/src/main/kotlin") {
        include("mdk.java-library-conventions.gradle.kts", "mdk.config-library.gradle.kts")
        into("resource-composition/conventions")
    }
    from("../../build-logic/src/main/kotlin/net/meatwo310/mdk/gradle") {
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
}
