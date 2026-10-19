package net.meatwo310.mdk.build

import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ResourceCompositionTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private val targets = listOf("neo", "legacy", "forge", "fabric", "disabled", "optional", "late")
    // Highest priority first: project precedence, then source-set precedence.
    private val inputs = listOf(
        Input("platform", "configClient"), Input("platform", "client"),
        Input("platform", "config"), Input("platform", "main"),
        Input("version", "configClient"), Input("version", "config"), Input("version", "main"),
        Input("common", "configClient"), Input("common", "config"), Input("common", "main"),
    )

    @Test
    fun `resources and classes remain consistent across archives and development builds`() {
        val root = temporary.newFolder("resource-regression")
        val cases = resourceCases()
        prepareBuild(root, cases)
        cases["renamed.txt"] = cases.remove("expanded.txt")!!
        val tasks = targets.flatMap { target ->
            listOf("jar", "sourcesJar", "stageModForDevelopment", "verifyRuntimeRoots").map { ":$target:$it" }
        }

        run(root, tasks)
        verify(root, cases)

        val reused = run(root, tasks)
        assertContains(reused.output, "Reusing configuration cache.")
        tasks.forEach { task ->
            // Verification tasks have no outputs and must execute on every build.
            val expectedOutcome = if (task.endsWith(":verifyRuntimeRoots")) TaskOutcome.SUCCESS else TaskOutcome.UP_TO_DATE
            assertEquals(expectedOutcome, reused.task(task)?.outcome, task)
        }
        verify(root, cases)

        targets.forEach { target ->
            root.resolve("$target/src/main/resources/deleted.txt").delete()
            inputs.filter { it.owner == "platform" }.forEach { input ->
                root.resolve("$target/src/${input.sourceSet}/resources/all.txt").delete()
            }
        }
        cases.remove("deleted.txt")
        cases["all.txt"] = inputs.filter { it.owner != "platform" }
        root.resolve("libraries-build/packages/config-core/src/main/java/fixture/RemovedCore.java").delete()
        write(root.resolve("libraries-build/packages/config-core/src/main/resources/core.txt"), "updated core\n")

        targets.forEach { target ->
            write(root.resolve("$target/src/main/resources/unique-platform-main.txt"), "updated\n")
            root.resolve("$target/src/configClient/java/fixture/platform_configClient.java").delete()
        }

        // Stage first, then archive: development runs must not contaminate later jars.
        run(root, targets.map { ":$it:stageModForDevelopment" } + tasks)
        verify(root, cases, changed = true)
        listOf("common", "version").forEach { owner ->
            assertTrue(root.resolve("${directory(owner)}/build/libs").walkTopDown().none { it.name.endsWith("-sources.jar") }, owner)
        }
    }

    @Test
    fun `cold development staging respects explicit priorities for arbitrary input names`() {
        val root = temporary.newFolder("explicit-inputs")
        val cases = resourceCases()
        prepareBuild(root, cases)
        root.resolve("build.gradle.kts").appendText("""

            project(":fabric") {
                val sets = extensions.getByType<SourceSetContainer>()
                val lower = sets.create("additionalAssets")
                val higher = sets.create("preferredAssets")
                includeModInputs(
                    localModInput(lower, ResourcePriority(-1, 10)),
                    localModInput(higher, ResourcePriority(-1, 0)),
                )
            }
        """.trimIndent() + "\n")
        write(root.resolve("fabric/src/additionalAssets/resources/all.txt"), "lower\n")
        write(root.resolve("fabric/src/preferredAssets/resources/all.txt"), "explicit winner\n")
        val stages = targets.map { ":$it:stageModForDevelopment" }
        val archives = targets.flatMap { target ->
            listOf("jar", "sourcesJar", "verifyRuntimeRoots").map { ":$target:$it" }
        }
        // No prior archive build: staging must be independently usable.
        run(root, stages)
        run(root, archives)
        cases["renamed.txt"] = cases.remove("expanded.txt")!!
        verify(root, cases, fabricResourceOverride = "explicit winner\n")
    }

    private fun resourceCases(): MutableMap<String, List<Input>> {
        val cases = linkedMapOf<String, List<Input>>()
        var pair = 0
        inputs.forEachIndexed { index, first ->
            inputs.drop(index + 1).forEach { second -> cases["pair-${pair++}.txt"] = listOf(first, second) }
        }
        cases["all.txt"] = inputs
        inputs.forEach { cases["unique-${it.owner}-${it.sourceSet}.txt"] = listOf(it) }
        listOf("deleted.txt", "expanded.txt", "generated.txt").forEach {
            cases[it] = listOf(Input("platform", "main"))
        }
        cases["assets/examplemod/probe.txt"] = inputs
        cases["data/examplemod/probe.txt"] = inputs
        return cases
    }

    private fun prepareBuild(root: File, cases: Map<String, List<Input>>) {
        write(root.resolve("settings.gradle.kts"), """
            rootProject.name = "resource-regression"
            includeBuild("libraries-build")
            include(":libraries:foundation", ":editions:shared", "external", ${targets.joinToString { "\"$it\"" }})
            project(":libraries:foundation").projectDir = file("inputs/shared-library")
            project(":editions:shared").projectDir = file("inputs/version-library")
        """.trimIndent() + "\n")
        listOf("libraries", "editions").forEach { root.resolve(it).mkdirs() }
        copyResource("build.gradle.kts", root.resolve("build.gradle.kts"))
        write(root.resolve("buildSrc/build.gradle.kts"), "plugins { `kotlin-dsl` }\nrepositories { mavenCentral() }\n")
        listOf("SourceSetArtifacts.kt", "ConfigSourceSets.kt", "ConfigCore.kt", "ResourceComposition.kt", "ModInput.kt", "SharedCode.kt").forEach { name ->
            copyResource("helpers/$name", root.resolve("buildSrc/src/main/kotlin/net/meatwo310/mdk/build/$name"))
        }
        write(root.resolve("libraries-build/settings.gradle.kts"), """
            rootProject.name = "library-fixture"
            include(":config-core")
            project(":config-core").projectDir = file("packages/config-core")
        """.trimIndent() + "\n")
        write(root.resolve("libraries-build/packages/config-core/build.gradle.kts"), """
            plugins { `java-library` }
            group = "fixture"
            version = "1.0"
            java.withSourcesJar()
        """.trimIndent() + "\n")
        write(root.resolve("libraries-build/packages/config-core/src/main/java/fixture/ConfigCore.java"), "package fixture; public class ConfigCore {}\n")
        write(root.resolve("libraries-build/packages/config-core/src/main/java/fixture/RemovedCore.java"), "package fixture; public class RemovedCore {}\n")
        write(root.resolve("libraries-build/packages/config-core/src/main/resources/core.txt"), "core\n")
        write(root.resolve("external/src/main/java/fixture/External.java"), "package fixture; public class External {}\n")
        write(root.resolve("${directory("common")}/src/main/java/fixture/Excluded.java"), "must not compile or be published\n")
        write(root.resolve("${directory("version")}/src/config/java/fixture/Excluded.java"), "must not compile or be published\n")
        cases.forEach { (name, candidates) ->
            candidates.forEach { input ->
                projects(input).forEach { project ->
                    write(root.resolve("$project/src/${input.sourceSet}/resources/$name"), input.content)
                }
            }
        }
        targets.forEach { target ->
            root.resolve("$target/src/main/resources/generated.txt").delete()
            write(root.resolve("$target/src/main/resources/excluded.txt"), "must not be packaged\n")
            write(root.resolve("$target/src/main/resources/expanded.txt"), "\${probe}\n")
            write(root.resolve("$target/templates/generated.txt"), "\${probe}\n")
        }
        inputs.forEach { input ->
            projects(input).forEach { project ->
                write(
                    root.resolve("$project/src/${input.sourceSet}/java/${input.className}.java"),
                    "package fixture; public class ${input.owner}_${input.sourceSet} { ${references(project, input)} }\n",
                )
            }
        }
    }

    private fun references(project: String, input: Input): String = when {
        input.owner == "common" && input.sourceSet == "config" -> "ConfigCore core;"
        input.owner == "version" && input.sourceSet == "main" -> "common_main shared;"
        input.owner == "version" && input.sourceSet == "config" -> "common_config shared;"
        input.owner == "platform" && input.sourceSet == "config" ->
            "common_config shared;" + if (project == "optional") "" else " version_config version;"
        input.owner == "platform" && input.sourceSet == "main" ->
            "version_main version; common_main shared;" + if (project == "disabled") "" else " platform_config config; ConfigCore core;"
        input.owner == "platform" && input.sourceSet == "configClient" -> "platform_config config;"
        else -> ""
    }

    private fun run(root: File, tasks: List<String>) = GradleRunner.create()
        .withProjectDir(root)
        .withArguments(tasks + listOf(
            "--no-parallel", "--max-workers=1", "--configuration-cache", "--configuration-cache-problems=fail",
            "--stacktrace", "-Dorg.gradle.jvmargs=-Xmx512m", "-Pkotlin.compiler.execution.strategy=in-process",
        ))
        .build()

    private fun verify(
        root: File,
        cases: Map<String, List<Input>>,
        changed: Boolean = false,
        fabricResourceOverride: String? = null,
    ) {
        targets.forEach { target ->
            val expected = cases.mapNotNull { (name, candidates) ->
                candidates.firstOrNull { enabled(target, it) }?.let {
                    val content = when {
                        target == "fabric" && name == "all.txt" && fabricResourceOverride != null -> fabricResourceOverride
                        changed && name == "unique-platform-main.txt" -> "updated\n"
                        else -> it.content
                    }
                    name to content
                }
            }.toMap() + if (target == "disabled") emptyMap() else mapOf("core.txt" to if (changed) "updated core\n" else "core\n")
            val expectedClasses = inputs.filter {
                enabled(target, it) && !(changed && it.owner == "platform" && it.sourceSet == "configClient")
            }.map { it.className }.toSet() + when {
                target == "disabled" -> emptySet()
                changed -> setOf("fixture/ConfigCore")
                else -> setOf("fixture/ConfigCore", "fixture/RemovedCore")
            }
            val stage = root.resolve("$target/build/mdk/developmentMod")
            val merged = root.resolve("$target/build/mdk/composedResources")
            ZipFile(root.resolve("$target/build/libs/$target.jar")).use { runtime ->
                ZipFile(root.resolve("$target/build/libs/$target-sources.jar")).use { sources ->
                    listOf(runtime, sources).forEach { archive ->
                        val entries = archive.entries().asSequence().toList()
                        assertEquals(entries.size, entries.map { it.name }.toSet().size, "$target: duplicate archive entries")
                        val actual = entries.filter { it.name.endsWith(".txt") }.associate { entry ->
                            entry.name to archive.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        }
                        assertEquals(expected, actual, "$target: ${archive.name}")
                    }
                    assertEquals(expectedClasses, archiveClasses(runtime, ".class"), "$target: runtime classes")
                    assertEquals(expectedClasses, archiveClasses(sources, ".java"), "$target: source classes")
                }
            }
            listOf(stage, merged).forEach { directory ->
                val actual = directory.walkTopDown().filter { it.isFile && it.extension == "txt" }.associate {
                    it.relativeTo(directory).invariantSeparatorsPath to it.readText()
                }
                assertEquals(expected, actual, "$target: $directory")
            }
            val stagedClasses = stage.walkTopDown().filter { it.isFile && it.extension == "class" }.map {
                it.relativeTo(stage).invariantSeparatorsPath.removeSuffix(".class")
            }.toSet()
            assertEquals(expectedClasses, stagedClasses, "$target: staged classes")
        }
    }

    private fun archiveClasses(archive: ZipFile, suffix: String) = archive.entries().asSequence()
        .map { it.name }.filter { it.endsWith(suffix) }.map { it.removeSuffix(suffix) }.toSet()

    private fun enabled(target: String, input: Input): Boolean = with(input) {
        if (owner != "platform") {
            sourceSet == "main" || (target != "disabled" && !(target == "optional" && owner == "version") &&
                (sourceSet != "configClient" || target in listOf("neo", "fabric")))
        } else {
            sourceSet == "main" || (sourceSet == "config" && target != "disabled") ||
                (sourceSet == "configClient" && target in listOf("neo", "fabric")) ||
                (sourceSet == "client" && target == "fabric")
        }
    }

    private fun projects(input: Input) = if (input.owner == "platform") targets else listOf(directory(input.owner))

    private fun directory(owner: String): String = when (owner) {
        "common" -> "inputs/shared-library"
        "version" -> "inputs/version-library"
        else -> owner
    }

    private fun copyResource(name: String, destination: File) {
        val content = requireNotNull(javaClass.getResourceAsStream("/resource-composition/$name")) {
            "Missing fixture resource: $name"
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }
        write(destination, content)
    }

    private fun write(file: File, content: String) {
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private data class Input(val owner: String, val sourceSet: String) {
        val content get() = "$owner:$sourceSet\n"
        val className get() = "fixture/${owner}_$sourceSet"
    }
}
