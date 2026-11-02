# Gradle plugins

m2mdk's Mod conventions live in `gradle-plugins/`. They are ordinary Gradle
plugins with IDs prefixed by `net.meatwo310.mdk.`. Consumers do not need
`buildSrc`. The sandbox is a complete example of a consuming multi-project build.
Use JDK 25 and Gradle 9.6.1 (the repository wrapper).

## Resolve the plugins

For a source checkout, add the plugin build in `settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("../m2mdk/gradle-plugins")
}
```

To publish plugin markers, implementations, and their build-helper dependency
to a Maven directory:

```sh
./gradlew -Pmdk.minecraft=none publishGradlePlugins \
  -Pmdk.pluginRepository=/absolute/path/to/maven-repository
```

The default destination is each build's `build/repository`; specify a shared
location when consuming published plugins. In the consumer's settings, use:

```kotlin
pluginManagement {
    repositories {
        maven("/absolute/path/to/maven-repository")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://nexus.gtnewhorizons.com/repository/public/") {
            content { includeGroupByRegex("com\\.gtnewhorizons(\\..+)?") }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
```

Then request a version on the root plugins and use the same IDs without versions
in subprojects:

```kotlin
plugins {
    id("net.meatwo310.mdk.mod-base-conventions") version "0.1.0-SNAPSHOT" apply false
    id("net.meatwo310.mdk.fabric-loom-mod-conventions") version "0.1.0-SNAPSHOT" apply false
}
```

The publication task writes Maven artifacts only; it does not publish to the
Gradle Plugin Portal or upload to a remote service.

## Configure a Mod

Apply `net.meatwo310.mdk.mod-base-conventions` to all shared and platform
subprojects. It sets the group, version, UTF-8 compilation, archive version,
and outgoing shared-source/resource variants. Set `modGroupId` in
`gradle.properties` and either `modVersion` or a root `version.txt`.

Before applying loader conventions, configure shared project paths and embedding
in the root build:

```kotlin
import net.meatwo310.mdk.build.sharedCode

subprojects {
    apply(plugin = "net.meatwo310.mdk.mod-base-conventions")
    sharedCode {
        commonProject.set(":common")
        versionProject.set(":26.1.2-common")
        embeddedPackage.set("example.mod.internal.mdk")
        configCoreDependency.set("net.meatwo310.mdk:config-core:0.1.0-SNAPSHOT")
        configDependencies.set(listOf("net.meatwo310.mdk:26.1.2-fabric-config:0.1.0-SNAPSHOT"))
        configUiDependencies.set(listOf("net.meatwo310.mdk:26.1.2-fabric-config-ui:0.1.0-SNAPSHOT"))
    }
}
```

Select config coordinates per project, as in `sandbox/build.gradle.kts`; shared
projects and other loaders must not receive Fabric coordinates. Set
`versionConfigEnabled` to `false` for 1.18.2/1.19.2. The 1.7.10 convention does
not consume shared config code. Add repositories for Minecraft, loader, and
optional dependencies in the consumer. During development, `includeBuild` of the
m2mdk root substitutes library coordinates; published consumers use Maven
repositories containing the selected libraries.

Mod metadata and loader versions remain consumer-owned Gradle properties.
Copy the properties, templates, and optional dependency declarations for the
chosen target from `sandbox/<minecraft>/<loader>/`; Mod IDs, package names,
entrypoints, URLs, and publishing project IDs belong to the consuming Mod.
A version catalog is optional. A `libs` catalog can override m2mdk's dependency
aliases and Fabric Loader/Mixin versions; the defaults are defined in
`VersionCatalogAccessors.kt`. Plugin implementation versions are pinned in
`gradle-plugins/gradle/libs.versions.toml`.

## Select conventions

Every ID below has the `net.meatwo310.mdk.` prefix.

| Minecraft | Loader | Mod convention | Config convention |
| --- | --- | --- | --- |
| 1.7.10 | Forge | `gtnh-mod-conventions` | none |
| 1.18.2, 1.19.2, 1.20.1 | Forge | `lexforge-legacy-mod-conventions` | `lexforge-legacy-config-conventions` |
| 1.21.1, 1.21.8, 1.21.11 | Forge | `lexforge-mod-conventions` | `lexforge-config-conventions` |
| 1.18.2, 1.19.2 | Fabric | `fabric-loom-remap-mod-conventions` | `fabric-legacy-config-conventions` |
| 1.20.1, 1.21.1, 1.21.8, 1.21.11 | Fabric | `fabric-loom-remap-mod-conventions` | `fabric-config-conventions` |
| 26.1, 26.1.2, 26.2, 26.3 | Fabric | `fabric-loom-mod-conventions` | `fabric-config-conventions` |
| 1.21.1, 26.1, 26.1.2, 26.2, 26.3 | NeoForge | `neoforge-mod-conventions` | `neoforge-config-conventions` |

Use `fabric-api-conventions` when using Fabric API. Shared projects use
`common-config-conventions` for global config code;
`lexforge-legacy-common-conventions` for 1.18.2–1.20.1 shared Minecraft code
(with `lexforge-legacy-common-config-conventions` on 1.20.1); and
`neoforge-common-conventions` plus `neoforge-common-config-conventions` on later
versions. `fabric-mod-conventions` is the shared base for the two Loom variants.

The optional root `mod-publish-conventions` plugin discovers projects with
`platformArtifacts` metadata, regardless of their names. Configure its
`modPublishing` extension for CurseForge/Modrinth and pass
`publishGitHubRepository`, `publishTag`, and optionally `publishProjects`
(comma-separated project paths without the leading colon). Publication defaults
to dry-run. See the sandbox root build for destination options.

## Verify

```sh
./gradlew -p gradle-plugins test --parallel --max-workers=2
CI=true ./sandbox/gradlew -p sandbox -Pmdk.minecraft=26.1.2 \
  :26.1.2-fabric:build --parallel --max-workers=2
```

Tests exercise a consumer with no `buildSrc` or version catalog, shared resources,
relocation, transitive embedding, and configuration-cache reuse. On an 8 GB
machine, keep Gradle workers at two and build Minecraft versions in batches;
loader tools may launch additional JVMs independently of Gradle workers.
