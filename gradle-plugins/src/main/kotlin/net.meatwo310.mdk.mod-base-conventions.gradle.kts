import net.meatwo310.mdk.build.exportModInputs
import net.meatwo310.mdk.build.sharedCode

// Apply to every shared and platform project before selecting loader conventions.
group = providers.gradleProperty("modGroupId").get()
val modVersion = providers.gradleProperty("modVersion")
    .orElse(providers.fileContents(rootProject.layout.projectDirectory.file("version.txt")).asText.map { it.trim() })
version = modVersion.get()
extensions.extraProperties["modVersion"] = version.toString()
sharedCode().embeddedPackage.convention(providers.gradleProperty("modGroupId").map { "$it.internal.mdk" })

tasks.withType<AbstractArchiveTask>().configureEach {
    archiveVersion.set(modVersion.map { "v$it" })
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
providers.gradleProperty("modId").orNull?.let { modId ->
    tasks.withType<Jar>().configureEach {
        from(rootProject.layout.projectDirectory.file("LICENSE")) {
            into("META-INF/$modId")
        }
    }
}
tasks.withType<JavaExec>().configureEach {
    standardInput = System.`in`
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
plugins.withId("java-library") {
    exportModInputs()
}
