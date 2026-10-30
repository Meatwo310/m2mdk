plugins {
    id("mdk.fabric-remap-config-library")
}

dependencies {
    api(project(":1.21.8-fabric-config"))
    api(project(":config-ui-core"))
    api(project(":1.21.8-common-config-ui"))
}
