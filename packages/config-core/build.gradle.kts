plugins {
    `java-library`
}

group = "net.meatwo310.mdk"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    withSourcesJar()
}

dependencies {
    compileOnlyApi("org.jspecify:jspecify:1.0.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

tasks.test {
    useJUnit()
    maxParallelForks = 1
    maxHeapSize = "256m"
}
