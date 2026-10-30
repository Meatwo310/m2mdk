plugins {
    id("mdk.neo-config-library")
}

dependencies {
    api(project(":26.1.2-neo-config"))
    api(project(":config-ui-core"))
    api(project(":26.1.2-common-config-ui"))
}
