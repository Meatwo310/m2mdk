plugins {
    id("mdk.config-library")
}

group = "net.meatwo310.mdk"
version = "0.1.0-SNAPSHOT"

dependencies {
    compileOnlyApi("org.jspecify:jspecify:1.0.0")
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
