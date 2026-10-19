pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "m2mdk"

include(":config-core")
project(":config-core").projectDir = file("packages/config-core")
