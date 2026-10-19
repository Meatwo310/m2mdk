plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = java.toolchain.languageVersion.map { it.asInt() }
}

tasks.withType<Test>().configureEach {
    maxParallelForks = 1
    maxHeapSize = "256m"
}
