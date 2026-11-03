# Gradle plugins

m2mdk owns reusable build behavior and its regression tests. A consumer owns its
Mod code, metadata, target selection, and explicit shared-project relationships.
It does not inherit this repository's Git history or test fixtures.

## Use source today

Use JDK 25 and the Gradle wrapper. Put the m2mdk checkout beside the consumer:

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("../m2mdk/gradle-plugins")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://nexus.gtnewhorizons.com/repository/public/")
    }
}
includeBuild("../m2mdk")
include(":shared")
include(":game-common", ":game-fabric")
project(":game-common").projectDir = file("26.1.2/common")
project(":game-fabric").projectDir = file("26.1.2/fabric")
```

The template supports `-Pm2mdk.path=/path/to/source` for both includes.
There is no cached checkout or bootstrap script. Updating the referenced source
updates the build implementation without merging template changes into the Mod.

The source library build defaults to 26.1.2, not all Minecraft environments.
Pass `-Pmdk.libraryMinecraft=1.21.1,26.1.2` when using other versions.
`none` includes core libraries only; `all` explicitly selects every version.
`mdk.minecraft` / `MDK_MINECRAFT` are shared selectors with the sandbox;
`mdk.libraryMinecraft` / `MDK_LIBRARY_MINECRAFT` select only library environments.
CLI options and environment variables propagate to composite builds; a property
in the consumer's gradle.properties is not a replacement for that selection.

## Configure before applying

Every participating project applies `mod-base-conventions`. It sets group and
version, encoding, reproducible archives, shared-output variants, and a root
LICENSE copy when `modId` is supplied. Set `modGroupId` and either `modVersion`
or root `version.txt`. The default embedded package is `<modGroupId>.internal.mdk`.

Configure each project's inputs before applying loader and Config conventions:

```kotlin
import net.meatwo310.mdk.build.sharedCode

plugins {
    id("net.meatwo310.mdk.mod-base-conventions")
    id("net.meatwo310.mdk.fabric-loom-mod-conventions") apply false
    id("net.meatwo310.mdk.fabric-config-conventions") apply false
    id("net.meatwo310.mdk.fabric-config-ui-conventions") apply false
}

sharedCode {
    commonProject.set(":shared")
    versionProject.set(":game-common")
    configLibraries("26.1.2", "fabric", "0.1.0-SNAPSHOT")
}
apply(plugin = "net.meatwo310.mdk.fabric-loom-mod-conventions")
apply(plugin = "net.meatwo310.mdk.fabric-config-conventions")
apply(plugin = "net.meatwo310.mdk.fabric-config-ui-conventions")
```

`commonProject` means Minecraft-independent global shared code; `versionProject`
means same-version loader-common code. These paths are arbitrary. Renaming a
project does not change its role, target Minecraft version, or library coordinates.
Missing shared paths fail with the consuming project and field name.

`configLibraries(minecraft, loader, version)` sets core, Config, and UI coordinates.
It does not apply plugins or resolve unused UI libraries. Loader is `common`,
`fabric`, `neo`, or `forge`. Only select coordinates that exist for that target.
For older versions without common Config, set `versionConfigEnabled` to false.
Individual dependency properties remain available for custom library selection.

The global shared project needs only `configCoreDependency`; version-common uses
`configLibraries(..., "common", ...)`. UI plugins require their corresponding
Config convention to have been explicitly applied first. To remove the screen,
remove the UI plugin and screen-registration sample. To remove Config as well,
remove Config plugins, library selection, declarations, and registration code.
There is no feature flag keeping unused code compiled.

Plugins declared with `apply false` do not provide Kotlin DSL extension
accessors. Use typed `extensions.configure<LoomGradleExtensionAPI>` or
`extensions.configure<NeoForgeExtension>` after applying the loader.

Mod metadata and Minecraft/Java/loader versions belong to the consumer's Gradle
properties and metadata templates. Keep version-specific values in each target's
gradle.properties when adding more Minecraft versions. A version catalog is
optional; default aliases are in `VersionCatalogAccessors.kt`.

## Select conventions

All plugin IDs below have prefix `net.meatwo310.mdk.`.

| Minecraft | Loader | Mod convention | Config convention |
| --- | --- | --- | --- |
| 1.7.10 | Forge | `gtnh-mod-conventions` | none |
| 1.18.2, 1.19.2, 1.20.1 | Forge | `lexforge-legacy-mod-conventions` | `lexforge-legacy-config-conventions` |
| 1.21.1, 1.21.8, 1.21.11 | Forge | `lexforge-mod-conventions` | `lexforge-config-conventions` |
| 1.18.2–1.21.11 | Fabric | `fabric-loom-remap-mod-conventions` | `fabric-legacy-config-conventions` on 1.18.2/1.19.2, otherwise `fabric-config-conventions` |
| 26.1, 26.1.2, 26.2, 26.3 | Fabric | `fabric-loom-mod-conventions` | `fabric-config-conventions` |
| 1.21.1, 26.1, 26.1.2, 26.2, 26.3 | NeoForge | `neoforge-mod-conventions` | `neoforge-config-conventions` |

Global sharing uses `common-config-conventions`, optionally
`common-config-ui-conventions`. Version-common uses
`neoforge-common-conventions` plus `neoforge-common-config-conventions` and
optional `neoforge-common-config-ui-conventions` on modern versions.
Older Forge common projects use `lexforge-legacy-common-conventions` and, on
1.20.1, `lexforge-legacy-common-config-conventions`.

Fabric and NeoForge platforms opt into UI with `fabric-config-ui-conventions`
and `neoforge-config-ui-conventions` respectively, where UI libraries exist.
Use `fabric-api-conventions` separately for Fabric API.

The optional root `mod-workspace-conventions` aggregates build/check/clean over
projects applying `mod-base-conventions`. Its `assembleRelease` collects only
declared `platformArtifacts`, regardless of project names, into
`build/release/<project-name>/`, including runtime Mod dependencies.
The optional `mod-publish-conventions` discovers the same metadata for
CurseForge/Modrinth publication, with dry-run as its default.

## Verify upstream, not downstream

```sh
./gradlew -p gradle-plugins test --max-workers=2
CI=true ../m2mdk-template/gradlew -p ../m2mdk-template build assembleRelease
python3 scripts/verify-consumer.py ../m2mdk-template
```

The verifier checks actual release jars, identity, sources, relocation, and
selected features without requiring sample classes or a fixed package.
Use `--without-ui` or `--without-config` for stripped consumer copies.
Template-specific tests remain here; they must not constrain downstream Mod edits.
For a disposable consumer checkout, after accepting the Minecraft EULA:

```sh
python3 scripts/consumer-smoke-server.py /path/to/disposable-consumer fabric --accept-eula
python3 scripts/consumer-smoke-server.py /path/to/disposable-consumer neoforge --accept-eula
```

## Future Maven consumption

Source composites and published libraries use the same coordinates. Publication
exists, but no public Maven endpoint is assumed or required today:

```sh
./gradlew -Pmdk.libraryMinecraft=none publishGradlePlugins \
  -Pmdk.pluginRepository=/absolute/path/to/repository
```

To migrate a consumer later, replace the plugin `includeBuild` with a Maven
repository holding both plugin markers and implementations, request a plugin
version, and replace the root source `includeBuild` with that library repository.
Update the plugin version and Config library version together. Publishing to a
remote service is separate from this local publication task.
