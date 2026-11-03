package net.meatwo310.mdk.build

import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PluginConsumptionTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `consumer applies conventions without buildSrc or version catalog`() {
        val root = temporary.newFolder("consumer")
        root.resolve("settings.gradle.kts").writeText("rootProject.name = \"consumer\"\n")
        root.resolve("gradle.properties").writeText("modGroupId=example.mod\nmodVersion=1.2.3\nmodName=Consumer\n")
        root.resolve("build.gradle.kts").writeText("""
            import net.meatwo310.mdk.build.*
            plugins {
                `java-library`
                id("net.meatwo310.mdk.mod-base-conventions")
                id("net.meatwo310.mdk.mod-publish-conventions")
            }
            java.withSourcesJar()
            val config = sourceSets.create("config")
            includeModInputs(localModInput(config, ResourcePriority(0, 0)))
            val loader = versionCatalog.library(VersionCatalogLibrary.FabricLoader).get()
            check(loader.group == "net.fabricmc" && loader.version == "0.19.5")
            check(versionCatalog.module(VersionCatalogLibrary.Minecraft) == "com.mojang:minecraft")
            check(configurations.findByName("mdkMainClasses") != null)
            check(group == "example.mod")
        """.trimIndent())
        root.resolve("src/main/java/example/Main.java").apply {
            parentFile.mkdirs()
            writeText("package example; public class Main {}\n")
        }
        root.resolve("src/config/java/example/Config.java").apply {
            parentFile.mkdirs()
            writeText("package example; public class Config {}\n")
        }
        fun run() = GradleRunner.create().withProjectDir(root).withPluginClasspath()
            .withArguments("build", "--configuration-cache", "--max-workers=1", "-Dorg.gradle.jvmargs=-Xmx512m")
            .build()
        run()
        assertContains(run().output, "Reusing configuration cache.")
        assertFalse(root.resolve("buildSrc").exists())
        ZipFile(root.resolve("build/libs/consumer-v1.2.3.jar")).use { jar ->
            assertTrue(jar.getEntry("example/Main.class") != null)
            assertTrue(jar.getEntry("example/Config.class") != null)
        }
        ZipFile(root.resolve("build/libs/consumer-v1.2.3-sources.jar")).use { jar ->
            assertEquals(2, jar.entries().asSequence().count { it.name.endsWith(".java") })
        }
    }

    @Test
    fun `workspace builds convention children and releases only declared platform artifacts`() {
        val root = temporary.newFolder("workspace")
        root.resolve("settings.gradle.kts").writeText("""
            rootProject.name = "workspace"
            include(":shipping:aurora", ":components:foundation", ":unrelated")
        """.trimIndent())
        root.resolve("gradle.properties").writeText("modGroupId=fixture\nmodVersion=1.2.3\nminecraftVersion=26.1\n")
        root.resolve("build.gradle.kts").writeText("""
            plugins { id("net.meatwo310.mdk.mod-workspace-conventions") }
        """.trimIndent())
        listOf("shipping/aurora", "components/foundation", "unrelated").forEach { directory ->
            root.resolve(directory).mkdirs()
        }
        root.resolve("shipping/aurora/build.gradle.kts").writeText("""
            import net.meatwo310.mdk.build.*
            plugins {
                `java-library`
                id("net.meatwo310.mdk.mod-base-conventions")
            }
            val distribution = tasks.register<Jar>("distributionJar") {
                archiveClassifier.set("distribution")
                from(sourceSets.main.get().output)
            }
            val distributionSources = tasks.register<Jar>("distributionSourcesJar") {
                archiveClassifier.set("distribution-sources")
                from(sourceSets.main.get().allSource)
            }
            val runtimeDependency = tasks.register<Jar>("runtimeDependency") {
                archiveFileName.set("runtime-dependency.jar")
                destinationDirectory.set(layout.buildDirectory.dir("ciRuntimeMods"))
                from("runtime.txt")
            }
            val unselected = tasks.register<Jar>("unselectedJar") { archiveClassifier.set("unselected") }
            configurePlatformArtifacts("fabric", JavaVersion.current().majorVersion.toInt(),
                "distributionJar", "distributionSourcesJar")
            tasks.assemble { dependsOn(distribution, distributionSources, runtimeDependency, unselected) }
        """.trimIndent())
        root.resolve("shipping/aurora/runtime.txt").writeText("runtime dependency\n")
        root.resolve("components/foundation/build.gradle.kts").writeText("""
            plugins {
                `java-library`
                id("net.meatwo310.mdk.mod-base-conventions")
            }
        """.trimIndent())
        root.resolve("unrelated/build.gradle.kts").writeText("plugins { `java-library` }\n")
        listOf("shipping/aurora" to "Platform", "components/foundation" to "Shared").forEach { (directory, name) ->
            root.resolve("$directory/src/main/java/fixture/$name.java").apply {
                parentFile.mkdirs()
                writeText("package fixture; public class $name {}\n")
            }
        }
        root.resolve("unrelated/src/main/java/Broken.java").apply {
            parentFile.mkdirs()
            writeText("This project must not be selected by the workspace lifecycle.\n")
        }

        val result = GradleRunner.create().withProjectDir(root).withPluginClasspath()
            .withTestKitDir(temporary.root.resolve("workspace-testkit"))
            .withArguments(":build", ":assembleRelease", "--no-parallel", "--max-workers=1",
                "--stacktrace", "-Dorg.gradle.jvmargs=-Xmx512m")
            .build()

        assertContains(result.tasks.map { it.path }, ":shipping:aurora:check")
        assertEquals(TaskOutcome.SUCCESS, result.task(":components:foundation:jar")?.outcome)
        assertEquals(null, result.task(":unrelated:compileJava"))
        val release = root.resolve("build/release")
        assertEquals(setOf(
            "aurora/aurora-v1.2.3-distribution.jar",
            "aurora/aurora-v1.2.3-distribution-sources.jar",
            "aurora/dependencies/runtime-dependency.jar",
        ), release.walkTopDown().filter { it.isFile }
            .map { it.relativeTo(release).invariantSeparatorsPath }.toSet())
        ZipFile(release.resolve("aurora/aurora-v1.2.3-distribution.jar")).use { jar ->
            assertTrue(jar.getEntry("fixture/Platform.class") != null)
        }
        ZipFile(release.resolve("aurora/aurora-v1.2.3-distribution-sources.jar")).use { jar ->
            assertTrue(jar.getEntry("fixture/Platform.java") != null)
        }
        ZipFile(release.resolve("aurora/dependencies/runtime-dependency.jar")).use { jar ->
            assertEquals("runtime dependency\n",
                jar.getInputStream(jar.getEntry("runtime.txt")).bufferedReader().use { it.readText() })
        }
    }
}
