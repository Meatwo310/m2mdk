plugins {
    id("mdk.fabric-config-library")
}

dependencies {
    api(project(":26.1-fabric-config"))
    api(project(":config-ui-core"))
    api(project(":26.1-common-config-ui"))
}
