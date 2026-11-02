plugins {
    base
}

// Build reusable libraries and Gradle plugins without including the Minecraft sandbox.
listOf("assemble", "check", "build", "clean").forEach { lifecycle ->
    tasks.named(lifecycle) {
        dependsOn(gradle.includedBuild("m2mdk-build-logic").task(":$lifecycle"))
        dependsOn(gradle.includedBuild("m2mdk-gradle-plugins").task(":$lifecycle"))
        dependsOn(subprojects.map { "${it.path}:$lifecycle" })
    }
}

tasks.register("publishGradlePlugins") {
    group = "publishing"
    description = "Publishes Gradle plugins, markers, and build helpers to mdk.pluginRepository."
    dependsOn(
        gradle.includedBuild("m2mdk-build-logic").task(":publishAllPublicationsToLocalRepository"),
        gradle.includedBuild("m2mdk-gradle-plugins").task(":publishAllPublicationsToLocalRepository"),
    )
}
