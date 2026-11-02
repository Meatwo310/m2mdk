import net.meatwo310.mdk.build.req

plugins {
    id("net.meatwo310.mdk.lexforge-legacy-mod-conventions")
    id("net.meatwo310.mdk.lexforge-legacy-config-conventions")
}

val configuredVersion = project.property("configuredVersion").toString()

// Mod Dependencies
dependencies {
    modRuntimeOnly(libs.configured, req(configuredVersion))
}
