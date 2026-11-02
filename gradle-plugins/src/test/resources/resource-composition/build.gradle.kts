import net.meatwo310.mdk.build.*

subprojects {
    apply(plugin = "java-library")
    exportModInputs()
    sharedCode { configCoreDependency.set("fixture:config-core:1.0") }
}
val shared = project(":libraries:foundation")
val versionProject = project(":editions:shared")
versionProject.dependencies.add("api", shared)
val sharedConfig = shared.configureConfigSourceSet()
shared.configureConfigSourceSet(CONFIG_CLIENT_SOURCE_SET_NAME).addConfigClasspath(sharedConfig)
versionProject.sharedCode { commonProject.set(shared.path) }
val versionConfig = versionProject.configureVersionCommonConfigSourceSet()
shared.includeConfigOutput(sharedConfig)
shared.extensions.getByType<SourceSetContainer>().named("main") { java.exclude("fixture/Excluded.java") }
versionConfig.java.exclude("fixture/Excluded.java")
subprojects.filter { it.name in setOf("neo", "legacy", "forge", "fabric", "disabled", "optional", "late") }.forEach { target ->
    with(target) {
        val sets = extensions.getByType<SourceSetContainer>()
        val java = extensions.getByType<JavaPluginExtension>()
        if (name != "late") java.withSourcesJar()
        sharedCode {
            commonProject.set(shared.path)
            versionProject.set(":editions:shared")
            versionConfigEnabled.set(name != "optional")
        }
        dependencies.add("implementation", versionProject)
        dependencies.add("runtimeOnly", project(":external"))
        includeSharedMainInputs()
        if (name == "fabric") {
            val client = sets.create("client")
            tasks.named<Jar>("jar") { from(client.output) }
            tasks.named<Jar>("sourcesJar") { from(client.allSource) }
            client.runtimeClasspath += configurations.getByName("runtimeClasspath")
        }
        if (name != "disabled") {
            // A separate runtime copy must be replaced by the embedded contribution.
            dependencies.add("runtimeOnly", sharedCode().configCoreDependency.get())
            val config = configureConfigSourceSet()
            val sharedSets = optionalSharedConfigInputs()
            val configSets = if (name in setOf("neo", "fabric")) {
                arrayOf(config, configureConfigSourceSet(CONFIG_CLIENT_SOURCE_SET_NAME))
            } else arrayOf(config)
            config.addConfigClasspath(sharedSets)
            configSets.drop(1).forEach { it.addConfigClasspath(sharedSets, config) }
            val sharedClient = if (name in setOf("neo", "fabric")) sharedConfigClientInputs(includeVersion = true) else emptyList()
            configSets.drop(1).forEach { client ->
                for (input in sharedClient) client.compileClasspath += input.output
            }
            includeConfigOutput(sharedSets, *configSets)
            includeModInputs(*sharedClient.toTypedArray())
            if (name == "fabric") {
                addConfigOutputTo("main", sharedSets, config)
                addConfigOutputTo("client", sharedSets, *configSets)
                addConfigInputsTo("client", *sharedClient.toTypedArray())
            } else if (name != "forge") {
                addConfigOutputTo("main", sharedSets, *configSets)
                addConfigInputsTo("main", *sharedClient.toTypedArray())
            } else {
                sets.named("main") {
                    for (input in sharedSets.including(project, *configSets)) compileClasspath += input.output
                }
            }
        }
        if (name == "late") java.withSourcesJar()
        val generated = tasks.register<Copy>("generateProbe") {
            from(layout.projectDirectory.dir("templates"))
            into(layout.buildDirectory.dir("generatedResources"))
            expand(mapOf("probe" to "platform:main"))
        }
        sets.named("main") { resources.srcDir(generated) }
        tasks.named<ProcessResources>("processResources") {
            filesMatching("expanded.txt") {
                expand(mapOf("probe" to "platform:main"))
                name = "renamed.txt"
            }
            exclude("excluded.txt")
        }
        developmentModSourceSet()
        // Persist paths as task inputs, avoiding Project/SourceSet access during execution.
        tasks.register("verifyRuntimeRoots") {
            val roots = listOfNotNull(sets.findByName("main"), sets.findByName("client"))
                .map { objects.fileCollection().from(it.runtimeClasspath) }
            val external = project(":external").tasks.named<Jar>("jar").flatMap { it.archiveFile }
            inputs.files(roots, external)
            val expected = layout.buildDirectory.dir("mdk/developmentMod").get().asFile
            doLast {
                for (classpath in roots) {
                    check(classpath.files == setOf(expected, external.get().asFile)) {
                        "Unexpected runtime roots: ${classpath.files}"
                    }
                }
            }
        }
        tasks.named<JavaCompile>("compileJava") {
            doFirst { check(source.files.none { "/src/config" in it.path.replace(File.separatorChar, '/') }) }
        }
    }
}
