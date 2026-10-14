plugins {
    `java-gradle-plugin`
}

group = "net.meatwo310.m2mdk"
version = "0.1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.neoforged:moddev-gradle:2.0.148")
    testImplementation(gradleTestKit())
}

val functionalTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks metadata generation in independent subprojects and configuration cache reuse."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("net.meatwo310.m2mdk.PluginSmokeTest")
    workingDir = layout.buildDirectory.dir("functional-tests").get().asFile
    doFirst { workingDir.mkdirs() }
}

tasks.check {
    dependsOn(functionalTest)
}

// The test sources are a TestKit executable, run by functionalTest.
tasks.test {
    enabled = false
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

gradlePlugin {
    plugins {
        create("neoforge") {
            id = "net.meatwo310.m2mdk.neoforge"
            implementationClass = "net.meatwo310.m2mdk.NeoForgePlugin"
            displayName = "m2mdk NeoForge"
            description = "Conventions for developing a NeoForge mod in a Gradle project."
        }
    }
}
