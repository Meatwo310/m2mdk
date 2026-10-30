package net.meatwo310.mdk.gradle

import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/** Inspect the artifacts players and developers actually consume. */
abstract class VerifyEmbeddedConfig : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val binaryJar: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sourcesJar: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val developmentDirectory: DirectoryProperty

    @get:Input
    abstract val embeddedPackage: Property<String>

    @get:Input
    abstract val requiredClasses: ListProperty<String>

    @get:Input
    abstract val includesUi: Property<Boolean>

    @TaskAction
    fun verify() {
        val prefix = embeddedPackage.get().replace('.', '/') + "/config/"
        val required = requiredClasses.get().map { prefix + it }
        ZipFile(binaryJar.get().asFile).use { jar ->
            val classes = jar.entries().asSequence().filter { it.name.endsWith(".class") }.toList()
            val names = classes.map { it.name }.toSet()
            required.forEach { check("$it.class" in names) { "Missing embedded class: $it" } }
            check(names.none { name ->
                listOf("net/meatwo310/mdk/", "net/minecraft/", "net/minecraftforge/", "net/neoforged/", "fuzs/")
                    .any(name::startsWith)
            }) { "The Mod JAR contains unrelocated libraries or bundled platform classes" }
            if (!includesUi.get()) {
                check(names.none { it.startsWith(prefix + "ui/") }) { "Config UI was bundled without being selected" }
            }
            classes.forEach { entry -> verifyReferences(entry.name, jar.getInputStream(entry).readBytes()) }
        }
        ZipFile(sourcesJar.get().asFile).use { jar ->
            required.forEach { check(jar.getEntry("$it.java") != null) { "Missing embedded source: $it" } }
            jar.entries().asSequence().filter { it.name.endsWith(".java") }.forEach { entry ->
                verifyReferences(entry.name, jar.getInputStream(entry).readBytes())
            }
        }
        val development = developmentDirectory.get().asFile
        required.forEach { check(development.resolve("$it.class").isFile) { "Missing development class: $it" } }
        development.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { file ->
            val bytes = file.readBytes()
            verifyReferences(file.relativeTo(development).path, bytes)
            check("net/minecraft/class_" !in bytes.toString(Charsets.ISO_8859_1)) {
                "Intermediary Minecraft references in development class ${file.relativeTo(development)}"
            }
        }
        logger.lifecycle("Verified relocated config libraries in distribution, sources, and development outputs")
    }

    private fun verifyReferences(name: String, bytes: ByteArray) {
        val text = bytes.toString(Charsets.ISO_8859_1)
        check("net/meatwo310/mdk/" !in text && "net.meatwo310.mdk." !in text) {
            "Unrelocated m2mdk reference in $name"
        }
    }
}
