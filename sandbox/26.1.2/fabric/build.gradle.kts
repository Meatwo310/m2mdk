import net.meatwo310.mdk.build.req

plugins {
    id("net.meatwo310.mdk.fabric-loom-mod-conventions")
    id("net.meatwo310.mdk.fabric-api-conventions")
    id("net.meatwo310.mdk.fabric-config-conventions")
}

val modmenuVersion = project.property("modmenuVersion").toString()

// Mod Dependencies
dependencies {
    runtimeOnly(libs.modmenu, req(modmenuVersion))
}
