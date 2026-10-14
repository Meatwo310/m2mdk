import java.util.zip.ZipFile
import org.tomlj.Toml

buildscript {
    repositories { mavenCentral() }
    dependencies { classpath("org.tomlj:tomlj:1.1.1") }
}

plugins {
    id("net.meatwo310.m2mdk.neoforge")
}

group = "net.meatwo310.m2mdk.sandbox"
version = "0.1.0"

m2mdk {
    modId.set("m2mdk_sandbox")
    modName.set("m2mdk Sandbox")
}

neoForge {
    version = "21.1.259"
}

abstract class VerifyModJar : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val modJar: RegularFileProperty

    @TaskAction
    fun verify() {
        val artifact = modJar.get().asFile
        ZipFile(artifact).use { jar ->
            val entryClass = "net/meatwo310/m2mdk/sandbox/SandboxMod.class"
            check(jar.getEntry(entryClass) != null) { "Missing mod entry class: $entryClass" }
            val entry = checkNotNull(jar.getEntry("META-INF/neoforge.mods.toml")) {
                "Missing META-INF/neoforge.mods.toml"
            }
            val metadata = jar.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            check(!metadata.contains("\${")) { "Unexpanded token in packaged mod metadata" }
            val parsed = Toml.parse(metadata)
            check(!parsed.hasErrors()) { "Invalid mod metadata: ${parsed.errors()}" }
            val mods = checkNotNull(parsed.getArray("mods")) { "Missing mod declarations" }
            check(mods.size() == 1) { "Expected one mod declaration" }
            val mod = mods.getTable(0)
            check(mod.getString("modId") == "m2mdk_sandbox") { "Unexpected mod ID" }
            check(mod.getString("version") == "0.1.0") { "Unexpected mod version" }
            check(mod.getString("displayName") == "m2mdk Sandbox") { "Unexpected mod display name" }
            check(parsed.getString("modLoader") == "javafml") { "Unexpected mod loader" }
            check(!parsed.getString("license").isNullOrBlank()) { "Missing license metadata" }
            val dependencies = checkNotNull(parsed.getArray("dependencies.m2mdk_sandbox")) {
                "Missing mod dependencies"
            }
            for ((id, range) in mapOf("minecraft" to "[1.21.1]", "neoforge" to "[21.1.259,)")) {
                check((0 until dependencies.size()).any { index ->
                    val dependency = dependencies.getTable(index)
                    dependency.getString("modId") == id &&
                        dependency.getString("type") == "required" &&
                        dependency.getString("versionRange") == range
                }) { "Missing or incorrect dependency: $id $range" }
            }
        }
        logger.lifecycle("Verified {}: mod entry class and expanded NeoForge metadata", artifact)
    }
}

val verifyModJar by tasks.registering(VerifyModJar::class) {
    group = "verification"
    description = "Verifies the sandbox JAR's entry class and NeoForge metadata."
    modJar.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })
}

tasks.check {
    dependsOn(verifyModJar)
}
