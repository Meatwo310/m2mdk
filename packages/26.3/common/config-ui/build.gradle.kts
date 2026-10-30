plugins {
    id("mdk.config-library")
}

dependencies {
    compileOnly("fuzs.forgeconfigapiport:forgeconfigapiport-common-neoforgeapi:${property("forgeConfigApiPortVersion")}")
    api(project(":26.3-common-config"))
    api(project(":config-ui-core"))
}
