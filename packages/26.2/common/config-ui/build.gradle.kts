plugins {
    id("mdk.config-library")
}

dependencies {
    compileOnly("fuzs.forgeconfigapiport:forgeconfigapiport-common-neoforgeapi:${property("forgeConfigApiPortVersion")}")
    api(project(":26.2-common-config"))
    api(project(":config-ui-core"))
}
