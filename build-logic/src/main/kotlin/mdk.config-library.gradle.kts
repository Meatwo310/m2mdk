import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.LibraryElements

plugins {
    id("mdk.java-library-conventions")
    `maven-publish`
}

group = "net.meatwo310.mdk"
version = "0.1.0-SNAPSHOT"

java.toolchain.languageVersion = JavaLanguageVersion.of(findProperty("javaVersion")?.toString()?.toInt() ?: 17)

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
    maven("https://maven.parchmentmc.org")
    exclusiveContent {
        forRepository { maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") }
        filter {
            includeGroupByRegex("fuzs\\..+")
            includeModule("net.minecraftforge", "forgeconfigapiport-fabric")
        }
    }
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
}

// Only explicit library API dependencies propagate into the embedded closure.
// Loader/Minecraft compile environments are deliberately absent from these variants.
val javaComponent = components["java"] as AdhocComponentWithVariants
listOf(false, true).forEach { sources ->
    val variant = configurations.create(if (sources) "mdkEmbeddedSources" else "mdkEmbeddedElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        extendsFrom(configurations.api.get())
        attributes {
            attribute(Attribute.of("net.meatwo310.mdk.embedded", String::class.java), "true")
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(if (sources) Category.DOCUMENTATION else Category.LIBRARY))
            if (sources) {
                attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
            } else {
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            }
        }
        outgoing.artifact(tasks.named(if (sources) "sourcesJar" else "jar"))
    }
    javaComponent.addVariantsFromConfiguration(variant) {
        mapToMavenScope("runtime")
    }
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) { into("META-INF/m2mdk") }
}

publishing {
    publications.create<MavenPublication>("library") {
        from(components["java"])
    }
}
