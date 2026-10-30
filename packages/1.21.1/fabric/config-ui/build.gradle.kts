plugins {
    id("mdk.fabric-remap-config-library")
}

dependencies {
    api(project(":1.21.1-fabric-config"))
    api(project(":config-ui-core"))
    api(project(":1.21.1-common-config-ui"))
}
