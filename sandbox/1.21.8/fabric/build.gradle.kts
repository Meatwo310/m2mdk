import net.meatwo310.mdk.build.req

plugins {
    id("net.meatwo310.mdk.fabric-loom-remap-mod-conventions")
    id("net.meatwo310.mdk.fabric-api-conventions")
    id("net.meatwo310.mdk.fabric-config-conventions")
    id("net.meatwo310.mdk.fabric-config-ui-conventions")
}

val modmenuVersion = project.property("modmenuVersion").toString()

// Mod Dependencies
dependencies {
    modRuntimeOnly(libs.modmenu, req(modmenuVersion))
}
