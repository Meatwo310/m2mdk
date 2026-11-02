import net.meatwo310.mdk.build.exportModInputs

// Apply to every shared and platform project before selecting loader conventions.
group = providers.gradleProperty("modGroupId").get()
val modVersion = providers.gradleProperty("modVersion")
    .orElse(providers.fileContents(rootProject.layout.projectDirectory.file("version.txt")).asText.map { it.trim() })
version = modVersion.get()
extensions.extraProperties["modVersion"] = version.toString()

tasks.withType<AbstractArchiveTask>().configureEach {
    archiveVersion.set(modVersion.map { "v$it" })
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
plugins.withId("java-library") {
    exportModInputs()
}
