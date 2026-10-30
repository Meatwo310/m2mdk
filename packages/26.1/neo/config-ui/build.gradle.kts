plugins {
    id("mdk.neo-config-library")
}

dependencies {
    api(project(":26.1-neo-config"))
    api(project(":config-ui-core"))
    api(project(":26.1-common-config-ui"))
}
