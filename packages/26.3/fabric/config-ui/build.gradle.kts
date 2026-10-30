plugins {
    id("mdk.fabric-config-library")
}

dependencies {
    api(project(":26.3-fabric-config"))
    api(project(":config-ui-core"))
    api(project(":26.3-common-config-ui"))
}
