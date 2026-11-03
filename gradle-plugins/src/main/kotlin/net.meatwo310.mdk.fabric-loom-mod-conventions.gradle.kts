import java.io.File
import net.meatwo310.mdk.build.*

plugins {
    id("net.meatwo310.mdk.fabric-mod-conventions")
    id("net.fabricmc.fabric-loom")
}

val modId = project.property("modId").toString()
val minecraftVersion = project.property("minecraftVersion").toString()

val developmentSourceSet = developmentModSourceSet()

loom {
    splitEnvironmentSourceSets()

    mods {
        create(modId) {
            sourceSet(developmentSourceSet)
        }
    }

    runs.configureEach {
        generateRunConfig.set(true)
        preferGradleTask.set(true)
        if (name == "gameTest") {
            jvmArguments.add("-Dfabric.log.level=debug")
        }
    }
}

dependencies {
    minecraft("${versionCatalog.module(VersionCatalogLibrary.Minecraft)}:$minecraftVersion")
    implementation(versionCatalog.library(VersionCatalogLibrary.FabricLoader))
}

// Loom materializes these while snapshotting the graph; clean may delete them.
tasks.withType<net.fabricmc.loom.task.AbstractRunTask>().configureEach {
    val launchClasspath = classpath
    val argumentFile = layout.buildDirectory.file("loom-cache/argFiles/$name")
    doFirst {
        val file = argumentFile.get().asFile
        file.parentFile.mkdirs()
        val paths = launchClasspath.files.joinToString(File.pathSeparator) {
            "\"" + it.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
        file.writeText("-classpath\n$paths", Charsets.UTF_8)
    }
}
