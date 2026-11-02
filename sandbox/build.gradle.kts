import groovy.json.JsonOutput
import net.meatwo310.mdk.gradle.VerifyEmbeddedConfig
import net.meatwo310.mdk.build.sharedCode
import net.meatwo310.mdk.build.platformArtifacts
import net.meatwo310.mdk.build.supportsGameTestServer
import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
    id("net.meatwo310.mdk.mod-publish-conventions")
    id("net.meatwo310.mdk.mod-base-conventions") apply false
}

modPublishing {
    curseForge {
        // projectId.set("123456")
        client.set(true)
        server.set(true)
    }
    modrinth {
        // projectId.set("xxxxxxxx")
        environment.set(CLIENT_AND_SERVER)
    }
}

tasks.named<Wrapper>("wrapper").configure {
    distributionType = Wrapper.DistributionType.BIN
}

tasks.register("writeCiBuildMatrix") {
    val outputFile = layout.buildDirectory.file("ci/build-matrix.json")
    val ciBuildProjects = (gradle.extensions.extraProperties["ciBuildProjectNames"] as List<*>)
        .map { it.toString() }
    val supportedLoaders = setOf("fabric", "forge", "neo")

    val duplicates = ciBuildProjects
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
    if (duplicates.isNotEmpty()) {
        throw GradleException("Duplicate CI build projects: ${duplicates.joinToString()}")
    }

    val ciBuildMatrix = ciBuildProjects.map { projectName ->
        val targetProject = project(":$projectName")
        evaluationDependsOn(targetProject.path)
        val artifacts = targetProject.platformArtifacts()
        val minecraftVersion = artifacts.minecraftVersion
        val loader = artifacts.loader
        val javaVersion = artifacts.javaVersion.toString()
        val fabricApiVersion = targetProject.findProperty("fabricApiVersion")
            ?.toString()
            ?.substringBefore("+")
            ?: "none"
        val runMcRuntimeTest = targetProject.findProperty("ciMcRuntimeTest")
            ?.toString()
            ?.toBooleanStrictOrNull()
            ?: true
        val requiresCommon = targetProject.findProperty("ciRequiresCommon")
            ?.toString()
            ?.toBooleanStrictOrNull()
            ?: true
        val mcRuntimeTest = when (loader) {
            "forge" -> "lexforge"
            "neo" -> "neoforge"
            else -> loader
        }
        val supportsGameTestServer = minecraftVersion.supportsGameTestServer()
        val commonProjectName = "$minecraftVersion-common"
        val expectedProjectName = "$minecraftVersion-$loader"

        if (loader !in supportedLoaders) {
            throw GradleException("Unsupported loader '$loader' in CI build project '$projectName'")
        }
        if (projectName != expectedProjectName) {
            throw GradleException(
                "CI build project '$projectName' declares platform '$expectedProjectName'",
            )
        }
        if (minecraftVersion.isBlank()) {
            throw GradleException("Project '$projectName' must define minecraftVersion")
        }
        if (requiresCommon && !rootProject.childProjects.containsKey(commonProjectName)) {
            throw GradleException("Project '$projectName' references missing common project '$commonProjectName'")
        }
        if (!javaVersion.matches(Regex("\\d+"))) {
            throw GradleException("Project '$projectName' has invalid javaVersion '$javaVersion'")
        }
        mapOf(
            "subproject" to projectName,
            "project_dir" to targetProject.projectDir.relativeTo(rootProject.projectDir).invariantSeparatorsPath,
            "loader" to loader,
            "minecraft" to minecraftVersion,
            "java" to javaVersion,
            "fabric_api" to fabricApiVersion,
            "supports_game_test_server" to supportsGameTestServer,
            "run_game_test_server" to (supportsGameTestServer && loader in setOf("forge", "neo")),
            "run_server" to !supportsGameTestServer,
            "run_mc_runtime_test" to runMcRuntimeTest,
            "modloader" to artifacts.modLoader,
            "mc_runtime_test" to mcRuntimeTest,
            "archive_base_name" to artifacts.archiveBaseName,
            "main_artifact" to artifacts.mainArtifactName,
            "sources_artifact" to artifacts.sourcesArtifactName,
            "release_artifacts" to artifacts.releaseArtifactNames,
            "modloader_regex" to ".*$loader.*",
        )
    }

    outputs.file(outputFile)
    inputs.property("ciBuildMatrix", ciBuildMatrix)

    doLast {
        val json = JsonOutput.toJson(ciBuildMatrix)
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(json)
        }
    }
}

with(System.getProperties()) {
    val version = get("java.version")
    val vmVersion = get("java.vm.version")
    val vendor = get("java.vendor")
    val arch = get("os.arch")
    println("Configuring with Java: $version, JVM: $vmVersion ($vendor), Arch: $arch")
}

subprojects {
    apply(plugin = "net.meatwo310.mdk.mod-base-conventions")
    // Repository layout policy lives here, outside reusable convention plugins.
    sharedCode {
        embeddedPackage.set("${project.property("modGroupId")}.internal.mdk")
        configCoreDependency.set("net.meatwo310.mdk:config-core:0.1.0-SNAPSHOT")
        commonProject.set(":common")
        if (name != "common" && name != "1.7.10-forge") {
            val minecraftVersion = name.substringBeforeLast('-')
            val loader = name.substringAfterLast('-')
            if (loader != "common" || minecraftVersion !in setOf("1.18.2", "1.19.2")) {
                configDependencies.set(listOf("net.meatwo310.mdk:$name-config:0.1.0-SNAPSHOT"))
            }
            if (loader != "forge" && minecraftVersion !in setOf("1.18.2", "1.19.2", "1.20.1")) {
                configUiDependencies.set(listOf("net.meatwo310.mdk:$name-config-ui:0.1.0-SNAPSHOT"))
            }
        }
        if (name != "common" && name != "1.7.10-forge") {
            versionProject.set(":${name.substringBeforeLast('-')}-common")
            versionConfigEnabled.set(name.substringBeforeLast('-') !in setOf("1.18.2", "1.19.2"))
        }
    }
    repositories {
        maven {
            name = "Minecraft Libraries"
            url = uri("https://libraries.minecraft.net")
        }

        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net/")
        }

        mavenCentral()

//        flatDir {
//            dir("libs")
//        }

        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://cursemaven.com")
                }
            }
            filter {
                includeGroup("curse.maven")
            }
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "Modrinth"
                    url = uri("https://api.modrinth.com/maven")
                }
            }
            filter {
                includeGroup("maven.modrinth")
            }
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "Fuzs Mod Resources"
                    url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
                }
            }
            filter {
                includeGroupByRegex("fuzs\\..+")
                includeModule("net.minecraftforge", "forgeconfigapiport-fabric")
            }
        }

        maven {
            name = "ParchmentMC"
            url = uri("https://maven.parchmentmc.org")
        }

//        maven {
//            name = "ModMaven"
//            url = uri("https://modmaven.dev/")
//        }
    }

    plugins.withType<JavaLibraryPlugin> {
        dependencies {
            add("compileOnlyApi", libs.jspecify)
        }
    }

    afterEvaluate {
        if (name != "common" && !name.endsWith("-common")) {
            if (name == "1.7.10-forge") {
                tasks.register("verifyEmbeddedConfig") { enabled = false }
            } else {
                val platform = extensions.getByType<net.meatwo310.mdk.build.PlatformArtifactsExtension>()
                val hasUi = sharedCode().configUiDependencies.get().isNotEmpty()
                val artifactNames = platformArtifacts()
                val verifyConfig = tasks.register<VerifyEmbeddedConfig>("verifyEmbeddedConfig") {
                    binaryJar.set(layout.buildDirectory.file("libs/${artifactNames.mainArtifactName}"))
                    dependsOn(tasks.named(platform.mainJarTaskName.get()))
                    sourcesJar.set(tasks.named<AbstractArchiveTask>(platform.sourcesJarTaskName.get()).flatMap { it.archiveFile })
                    developmentDirectory.set(layout.buildDirectory.dir("mdk/developmentMod"))
                    embeddedPackage.set(sharedCode().embeddedPackage)
                    includesUi.set(hasUi)
                    requiredClasses.set(listOf("ConfigEntry", "ConfigDeclaration", "adapter/VersionedConfigSpec", "adapter/PlatformConfigRegistrar") +
                        if (hasUi) listOf("ui/PlatformConfigScreenRegistrar", "ui/KeyedConfigScreen", "ui/ConfigReadableNames", "ui/ConfigTranslationKeys", "ui/ConfigScreenTranslations") else emptyList())
                    dependsOn(tasks.named("stageModForDevelopment"), tasks.named("mdkDevelopmentClasses"))
                    tasks.findByName("reobfJar")?.let { dependsOn(it) }
                }
                tasks.named("check") { dependsOn(verifyConfig) }
            }
        }
        tasks.withType<JavaExec>().configureEach {
            standardInput = System.`in`
        }
    }

    plugins.withId("idea") {
        configure<IdeaModel> {
            module {
                isDownloadSources = true
                isDownloadJavadoc = true
            }
        }
    }
}
