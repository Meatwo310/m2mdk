import net.meatwo310.mdk.build.req

plugins {
    id("net.meatwo310.mdk.fabric-loom-remap-mod-conventions")
    id("net.meatwo310.mdk.fabric-api-conventions")
    id("net.meatwo310.mdk.fabric-config-conventions")
}

val forgeconfigscreensVersion = project.property("forgeconfigscreensVersion").toString()
val modmenuVersion = project.property("modmenuVersion").toString()

// Mod Dependencies
dependencies {
    modRuntimeOnly(libs.forgeconfigscreens, req(forgeconfigscreensVersion))
    modRuntimeOnly(libs.modmenu, req(modmenuVersion))
}
