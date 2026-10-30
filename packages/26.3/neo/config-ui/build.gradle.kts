plugins {
    id("mdk.neo-config-library")
}

dependencies {
    api(project(":26.3-neo-config"))
    api(project(":config-ui-core"))
    api(project(":26.3-common-config-ui"))
}
