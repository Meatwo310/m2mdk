plugins {
    id("mdk.config-library")
}

dependencies {
    compileOnly("fuzs.forgeconfigapiport:forgeconfigapiport-common:${property("forgeConfigApiPortVersion")}")
    api(project(":config-core"))
}
