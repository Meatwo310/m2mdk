plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.minecraftforge.net/")
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.2.2")
    implementation("net.fabricmc:fabric-loom:1.17.14")
    implementation("net.neoforged:moddev-gradle:2.0.147")
}

group = "net.meatwo310.mdk"
version = "0.1.0-SNAPSHOT"
