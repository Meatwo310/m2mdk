import net.meatwo310.mdk.build.platformArtifacts
import net.meatwo310.mdk.build.PlatformArtifactsExtension

plugins {
    base
}

require(project == rootProject) { "mod-workspace-conventions must be applied to the root project" }

val release = tasks.register<Sync>("assembleRelease") {
    group = "distribution"
    description = "Collects declared platform artifacts and their runtime Mod dependencies."
    into(layout.buildDirectory.dir("release"))
}

subprojects {
    val target = this
    plugins.withId("net.meatwo310.mdk.mod-base-conventions") {
        listOf("assemble", "check", "build", "clean").forEach { lifecycle ->
            rootProject.tasks.named(lifecycle) { dependsOn("${target.path}:$lifecycle") }
        }
    }
}

// Match publishing: discover explicit artifact metadata after projects configure.
gradle.projectsEvaluated {
    subprojects.filter { it.extensions.findByType(PlatformArtifactsExtension::class.java) != null }.forEach { target ->
        val artifacts = target.platformArtifacts()
        release.configure {
            dependsOn("${target.path}:build")
            from(artifacts.releaseArtifactNames.map { target.layout.buildDirectory.file("libs/$it") }) {
                into(target.name)
            }
            from(target.layout.buildDirectory.dir("ciRuntimeMods")) { into("${target.name}/dependencies") }
        }
    }
}
