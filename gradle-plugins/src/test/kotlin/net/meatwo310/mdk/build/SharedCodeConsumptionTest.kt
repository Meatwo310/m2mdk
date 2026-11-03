package net.meatwo310.mdk.build

import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class SharedCodeConsumptionTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `explicit library selection embeds config without requiring UI unless opted in`() {
        listOf(false, true).forEach { ui ->
            val root = temporary.newFolder(if (ui) "with-ui" else "config-only")
            val minecraft = if (ui) "26.1" else "1.21.1"
            val loader = if (ui) "neo" else "fabric"
            val modules = listOf("config-core", "$minecraft-$loader-config") +
                if (ui) listOf("$minecraft-$loader-config-ui") else emptyList()
            write(root.resolve("settings.gradle.kts"), """
                rootProject.name = "unrelated-name"
                includeBuild("libraries")
            """.trimIndent())
            write(root.resolve("gradle.properties"), "modGroupId=fixture\nmodVersion=1.2.3\n")
            write(root.resolve("libraries/settings.gradle.kts"), """
                rootProject.name = "local-libraries"
                include(${modules.joinToString { "\"$it\"" }})
            """.trimIndent())
            modules.forEach { module ->
                write(root.resolve("libraries/$module/build.gradle.kts"), """
                    plugins { `java-library` }
                    group = "net.meatwo310.mdk"
                    version = "9.8.7"
                    java.withSourcesJar()
                """.trimIndent())
            }
            write(root.resolve("libraries/config-core/src/main/java/library/Core.java"),
                "package library; public class Core {}\n")
            write(root.resolve("libraries/$minecraft-$loader-config/src/main/java/library/Adapter.java"),
                "package library; public class Adapter {}\n")
            if (ui) {
                write(root.resolve("libraries/$minecraft-$loader-config-ui/src/main/java/library/Screen.java"),
                    "package library; public class Screen {}\n")
            }
            write(root.resolve("build.gradle.kts"), """
                import net.meatwo310.mdk.build.*
                plugins {
                    `java-library`
                    id("net.meatwo310.mdk.mod-base-conventions")
                    id("net.meatwo310.mdk.common-config-conventions") apply false
                    id("net.meatwo310.mdk.common-config-ui-conventions") apply false
                    id("net.meatwo310.mdk.neoforge-common-config-ui-conventions") apply false
                    id("net.meatwo310.mdk.fabric-config-ui-conventions") apply false
                    id("net.meatwo310.mdk.neoforge-config-ui-conventions") apply false
                }
                sharedCode { configLibraries("$minecraft", "$loader", "9.8.7") }
                apply(plugin = "net.meatwo310.mdk.common-config-conventions")
                ${if (ui) "apply(plugin = \"net.meatwo310.mdk.common-config-ui-conventions\")" else ""}
                java.withSourcesJar()
                developmentModSourceSet()
            """.trimIndent())
            write(root.resolve("src/config/java/fixture/Config.java"), """
                package fixture;
                public class Config { library.Core core; library.Adapter adapter; }
            """.trimIndent())
            // This must not even compile in the config-only build: its library is unavailable.
            write(root.resolve("src/configClient/java/fixture/Client.java"), """
                package fixture;
                public class Client { Config config; library.Screen screen; }
            """.trimIndent())

            val tasks = listOf("jar", "sourcesJar", "stageModForDevelopment") +
                if (ui) listOf("compileConfigClientJava") else emptyList()
            runner(root, *tasks.toTypedArray()).build()

            val expected = setOf("fixture/Config", "library/Core", "library/Adapter") +
                if (ui) setOf("library/Screen") else emptySet()
            // Common UI sources are exported for platform consumers, not added to the common jar.
            assertEquals(ui, root.resolve("build/classes/java/configClient/fixture/Client.class").isFile)
            listOf("" to ".class", "-sources" to ".java").forEach { (classifier, suffix) ->
                ZipFile(root.resolve("build/libs/unrelated-name-v1.2.3$classifier.jar")).use { jar ->
                    assertEquals(expected, jar.entries().asSequence().map { it.name }
                        .filter { it.endsWith(suffix) }.map { it.removeSuffix(suffix) }.toSet())
                }
            }
            val development = root.resolve("build/mdk/developmentMod")
            assertEquals(expected, development.walkTopDown().filter { it.extension == "class" }
                .map { it.relativeTo(development).invariantSeparatorsPath.removeSuffix(".class") }.toSet())
        }
    }

    @Test
    fun `missing shared paths identify the consuming project and configuration field`() {
        listOf("commonProject", "versionProject").forEach { field ->
            listOf(false, true).forEach { nonexistent ->
                val root = temporary.newFolder("$field-$nonexistent")
                write(root.resolve("settings.gradle.kts"), """
                    rootProject.name = "missing-paths"
                    include(":inputs:foundation", ":editions:shared", ":arbitrary-consumer")
                """.trimIndent())
                write(root.resolve("build.gradle.kts"), """
                    import net.meatwo310.mdk.build.*
                    plugins { id("net.meatwo310.mdk.mod-base-conventions") apply false }
                    project(":arbitrary-consumer") {
                        apply(plugin = "java-library")
                        sharedCode {
                            commonProject.set(":inputs:foundation")
                            versionProject.set(":editions:shared")
                            $field.${if (nonexistent) "set(\":absent:shared\")" else "unset()"}
                        }
                        includeSharedMainInputs()
                    }
                """.trimIndent())
                listOf("inputs/foundation", "editions/shared", "arbitrary-consumer").forEach {
                    root.resolve(it).mkdirs()
                }

                val result = runner(root, "help").buildAndFail()

                assertContains(result.output, ":arbitrary-consumer")
                assertContains(result.output, "sharedCode.$field")
                if (nonexistent) assertContains(result.output, ":absent:shared")
            }
        }
    }

    private fun runner(root: File, vararg tasks: String) = GradleRunner.create()
        .withProjectDir(root)
        .withPluginClasspath()
        .withTestKitDir(temporary.root.resolve("testkit"))
        .withArguments(tasks.toList() + listOf(
            "--no-parallel", "--max-workers=1", "--stacktrace",
            "-Dorg.gradle.jvmargs=-Xmx512m",
        ))

    private fun write(file: File, content: String) {
        file.parentFile.mkdirs()
        file.writeText(content)
    }
}
