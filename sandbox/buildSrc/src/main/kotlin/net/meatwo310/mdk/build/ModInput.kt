package net.meatwo310.mdk.build

import org.gradle.api.file.FileCollection

/** Lower numbers win. Neither project paths nor source-set names carry priority. */
data class ResourcePriority(val layer: Int, val withinLayer: Int)

/** File-based boundary between producers, composition, and development runs. */
data class ModInput(
    val id: String,
    val classes: FileCollection,
    val resources: FileCollection,
    val sources: FileCollection,
    val resourceRoots: FileCollection,
    val replacedRuntimeArtifacts: FileCollection,
    val priority: ResourcePriority,
) {
    val output: FileCollection get() = classes + resources
}
