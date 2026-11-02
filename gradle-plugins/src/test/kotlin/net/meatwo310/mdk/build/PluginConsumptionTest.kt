package net.meatwo310.mdk.build

import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
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
}
