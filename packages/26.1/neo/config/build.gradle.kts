plugins {
    id("mdk.neo-config-library")
}

dependencies {
    api(project(":config-core"))
    api(project(":26.1-common-config"))
}
