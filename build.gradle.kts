plugins {
    base
}

// Aggregate library lifecycle tasks without including the Minecraft sandbox.
listOf("assemble", "check", "build", "clean").forEach { lifecycle ->
    tasks.named(lifecycle) {
        dependsOn(subprojects.map { "${it.path}:$lifecycle" })
    }
}
