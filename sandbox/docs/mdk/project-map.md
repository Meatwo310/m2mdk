# Project Map

This file maps repository locations to responsibilities.

Paths below are relative to sandbox unless stated otherwise.

## Independent Packages

`../packages/config-core`

- Independent Java 17 library build, wrapper, sources jar, and contract tests.
- Owns `net.meatwo310.mdk.config` declarations, values, and binding contracts.
- Has no dependency on sandbox, Minecraft, loaders, or client UI.
- Included by sandbox settings as a composite build; consumed as a normal
  binary/sources dependency and embedded by the config conventions.

## Root

`settings.gradle.kts`

- Includes every Gradle subproject that is enabled in this repository.
- Defines `ciBuildProjectNames` as all children except `common` and `*-common`.
- Controls which loader projects enter the build and release matrices.

`build.gradle.kts`

- Applies loader Gradle plugins at the root with `apply false`.
- Registers `writeCiBuildMatrix`.
- Reads each loader project's `platformArtifacts` metadata and records exact
  main, sources, and release artifact names in the CI matrix.
- Honors a loader project's `ciRequiresCommon=false` property when a standalone
  project does not have a matching `<minecraft>-common` project.
- Reads `version.txt` into `modVersion`.
- Supplies `sharedCode` project paths and optional version-config participation.
- Exports source-set directory artifacts for explicit project dependencies.
- Sets common subproject group, version, repositories, Java compile encoding,
  archive version, and IDEA behavior.

`gradle.properties`

- Global mod metadata and Fabric entrypoint defaults.

`version.txt`

- Single release version used by all subprojects.

`gradle/libs.versions.toml`

- Shared plugin and dependency aliases.

## Shared Code

`common/src/main`

- Cross-version Java used by convention-backed loaders and Minecraft versions.
- The standalone 1.7.10 Forge project does not consume it.

`common/src/config`

- Editable example config declarations; framework core is outside sandbox.

`common/src/configClient`

- Loader-neutral UI display helpers, separate from config core.

`<mc>/common/src/main`

- Java shared by all loaders for one Minecraft version.
- Use this for API differences tied to a Minecraft version.

`<mc>/common/src/config`

- Version-specific Spec adapters in `net.meatwo310.mdk.config.adapter`.

`<mc>/common/src/configClient`

- Spec-dependent UI helpers, consumed only by client config integrations.

## Loader Projects

`<mc>/fabric`

- Fabric loader entrypoint and Fabric-specific integration.
- Uses either `fabric-loom-mod-conventions` or
  `fabric-loom-remap-mod-conventions`.
- Usually applies `fabric-api-conventions` and the matching config convention:
  `fabric-legacy-config-conventions` for 1.18.2-1.19.2 or
  `fabric-config-conventions` for 1.20.1 and newer.

`<mc>/forge`

- LexForge entrypoint, resources, mixin config, templates, and runtime deps.
- Uses `lexforge-mod-conventions` for ForgeGradle 7+ or `lexforge-legacy-mod-conventions` for older versions.
- Applies the matching `lexforge-config-conventions` or `lexforge-legacy-config-conventions` when config code is included.
- `1.7.10/forge` is a standalone exception: it uses GTNHGradle, keeps
  dependencies and repositories in project-local Gradle scripts, and has no
  matching `1.7.10/common` project.

`<mc>/neo`

- NeoForge entrypoint, resources, templates, and runtime deps.
- Uses `neoforge-mod-conventions`.
- Applies `neoforge-config-conventions` when config code is included.

## Source Sets

`src/main/java`

- Loader-specific main code.
- Loader entrypoints live here unless a convention says otherwise.

`src/client/java`

- Fabric client source set, including Fabric `ModClient`.

`src/config/java`

- Config implementation source set.

`src/configClient/java`

- Client config UI source set.

`src/main/resources`

- Static loader resources, such as mixin JSON files and the GTNHGradle-backed
  1.7.10 project's `mcmod.info`.

`src/main/templates`

- Loader metadata templates.
- Fabric: `fabric.mod.json`.
- LexForge and LexForge Legacy: `META-INF/mods.toml`.
- NeoForge: `META-INF/neoforge.mods.toml`.

`src/generated/resources`

- Generated data resources for Forge and NeoForge data runs.

## buildSrc

`buildSrc/src/main/kotlin/*conventions.gradle.kts`

- Precompiled Gradle convention plugins.

`common-config-conventions`

- Adds `src/config` to `common` and packages its output.

`lexforge-legacy-common-conventions`, `neoforge-common-conventions`

- Configure one-version common projects for LexForge Legacy or NeoForge tooling.

`lexforge-mod-conventions`, `lexforge-legacy-mod-conventions`, `neoforge-mod-conventions`

- Configure runs, metadata generation, Java toolchain, archives, and jar
  contents for Forge and NeoForge.

`fabric-mod-conventions`

- Configures Fabric metadata generation, Java toolchain, archives, and jar
  contents.

`fabric-loom-mod-conventions`, `fabric-loom-remap-mod-conventions`

- Configure Loom, split client source sets, Minecraft dependencies, mappings,
  and shared source sets.

`fabric-api-conventions`

- Adds Fabric API dependency and game test setup where supported.

`*-config-conventions`

- Wire `src/config` and `src/configClient` into classpaths and jars.

`net/meatwo310/mdk/build`

- Kotlin helpers for source sets, CI runtime mod staging, Fabric metadata,
  dependency version actions, version support, and version catalog access.

`ModInput.kt`, `SharedCode.kt`

- Defines file-based `ModInput` contributions and explicit resource priorities.
- Exports classes, processed resources, filtered sources, and raw resource roots via
  consumable configurations. Producer task dependencies travel with outputs.
- Stages source files with their original include/exclude rules and omits raw
  resources; processed resources are supplied by composition.
- Consumes shared contributions through project dependencies without looking up
  another project's source sets or forcing its evaluation.
- `sharedCode` takes explicit common/version project paths. Repository defaults
  are wired in the root build, not inferred inside reusable conventions.
- `versionConfigEnabled=false` explicitly omits version config for optional
  config integrations; a required config integration still requires its artifacts.

`ConfigCore.kt`, `ConfigSourceSets.kt`

- Resolve ordinary binary and sources variants for `sharedCode.configCoreDependency`.
- Embed core as a low-priority `ModInput`, replacing its separate runtime jar
  with the staged mod root for development runs.
- Keep common/version `configClient` inputs separate from declarations, so
  Fabric and NeoForge consume UI helpers without adding them to LexForge.

`ResourceComposition.kt`, `SourceSetArtifacts.kt`

- Compose contributions using supplied priorities, independent of project names.
- Share processed resources across distribution jars, sources jars, and staged runs.
- Replace contributed runtime outputs/artifacts while preserving external dependencies.
- Local source-set naming policy is adapted separately from composition inputs.

`PlatformArtifacts.kt`

- Defines loader identity, Java version, publishing dependencies, and
  the Gradle tasks that produce each platform's main and optional sources jars.
- Provides the `platformArtifacts { publishingDependencies { ... } }` DSL for
  CurseForge and Modrinth project-page relations.
- Reads the archive base name from the platform's Gradle `base` configuration.
- Resolves exact archive file names for CI, release collection, and downstream
  publishing logic.

`mod-publish-conventions.gradle.kts`

- Configures CurseForge and Modrinth publications from `platformArtifacts`.
- Exposes Mod Publish Plugin's native CurseForge and Modrinth options through the `modPublishing` block.
- Downloads the selected GitHub Release instead of rebuilding its jars.
- Supports destination, platform subset, release type, and dry-run inputs.

`DependencyVersionConstraints.kt`

- Provides `req(version)` for `version { require(version) }` and `pin(version)`
  for `version { strictly(version) }`.

`CiRuntimeModsConvention.kt`

- Creates non-transitive `ciRuntimeMods` and `collectCiRuntimeMods`.
- Stages direct CI runtime-test jars under each configured project directory's `build/ciRuntimeMods`.

## CI And Release

`.github/workflows/sandbox.yml` (repository root)

- Detects platform projects with `writeCiBuildMatrix`.
- Tests the independent config core and sandbox build logic before the matrix.
- Runs on pushes to `main` and `main2`, pull requests, and manual dispatch.
- Saves per-platform Gradle caches on trusted runs; pull requests read caches only.
- Uploads jars and runtime mod jars, then runs game/server/runtime tests.

`.github/workflows/release.yml`

- Manually bumps or keeps `version.txt`.
- Builds all platform projects from the CI matrix.
- Collects the exact release artifacts declared by each platform, generates
  release notes, tags, and publishes a GitHub Release.

`.github/workflows/publish.yml`

- Publishes an existing GitHub Release to CurseForge and/or Modrinth.
- Defaults to dry-run mode and can restrict publishing to selected platform
  projects.
