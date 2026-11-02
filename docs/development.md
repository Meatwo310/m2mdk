# Library development

Use JDK 25 to run Gradle. The Foojay Toolchain Resolver downloads additional JDKs
as needed. If you use Nix, run `nix develop` to enter the development environment.

## Config libraries

Config implementations live under `packages/`, independently of the sandbox:

```text
packages/
  config-core/                 # Minecraft-independent declarations and binding contracts
  config-ui-core/              # Minecraft-independent UI naming helpers
  <minecraft>/<loader>/config/
  <minecraft>/<loader>/config-ui/
```

`loader` is `common`, `forge`, `neo`, or `fabric`. Each leaf is a Gradle
subproject named `<minecraft>-<loader>-<feature>`. A `common` module exists only
where implementations can be shared within that Minecraft version. Versions
remain independent. UI modules are available only where the sandbox previously
provided a custom config screen; no config framework is added to 1.7.10.

These are ordinary Java libraries, without Mod metadata. Loader-specific config
modules depend on their common module and `config-core`; optional UI modules add
their UI dependencies. They compile against platform APIs without embedding them.
Fabric and modern Forge consumers still need Forge Config API Port at runtime.

Config registration is exposed through `PlatformConfigRegistrar`, with binding
provided by `VersionedConfigSpec`. UI modules expose
`PlatformConfigScreenRegistrar.register(...)`; callers pass a Fabric Mod ID or
NeoForge `ModContainer`. Loader-specific initialization signatures are preserved.

`ConfigEntryBuilder.defineListAllowEmpty(...)` accepts the same default
list, new-element supplier, and validator as `defineList(...)`. Use it when a
list may be empty in NeoForge-style config screens. Other implementations,
including Configured and direct file editing, currently allow empty lists with
either method.

The sandbox contains editable config declarations and initialization calls. It
consumes the libraries through `includeBuild("..")`, using the same coordinates
as a published library: `net.meatwo310.mdk:<module>:0.1.0-SNAPSHOT`.

The reusable helpers in `build-logic` resolve the selected libraries and their
transitive m2mdk dependencies through `mdkEmbeddedElements` and
`mdkEmbeddedSources` variants. These variants are also published in Gradle module
metadata; they retain the development namespace for mapped Minecraft versions.
The sandbox's `sharedCode` configuration selects config and optional UI modules,
and supplies the destination package through `embeddedPackage`.

Source imports keep using `net.meatwo310.mdk`. During assembly, both the library
classes and the Mod's references are relocated under the consumer's package
(`net.meatwo310.examplemod.internal.mdk` in the sandbox), before loader remapping
or reobfuscation. Development runs and source JARs use the same relocated package.
Minecraft, loader, and FCAP classes are not bundled. Libraries retain their MIT
license under `META-INF/m2mdk/LICENSE` in the consuming Mod.

## Build and verification

Build every library with `./gradlew build`, or limit the Minecraft environments
configured in a local build:

```sh
./gradlew -Pmdk.minecraft=26.1.2 build --no-parallel --max-workers=1
./gradlew -Pmdk.minecraft=none :config-core:build :config-ui-core:build
```

To avoid Minecraft decompilation during verification, set `CI=true`. ModDevGradle uses
patched game binaries instead of decompiling and recompiling Minecraft. This
does not skip compilation or source JAR generation for m2mdk or the sandbox Mod.
Leave it unset when Minecraft sources are needed for debugging.

The sandbox is a separate integration build. For example:

```sh
./sandbox/gradlew -p sandbox -Pmdk.minecraft=26.1.2 :26.1.2-fabric:build --no-parallel --max-workers=1
./sandbox/gradlew -p sandbox/buildSrc test --no-parallel --max-workers=1
```

`mdk.minecraft` accepts a comma-separated version list and limits both builds.
`mdk.libraryMinecraft` can limit only the library build, for tasks such as
sandbox matrix discovery. CI uses the equivalent `MDK_MINECRAFT` and
`MDK_LIBRARY_MINECRAFT` environment variables.

Each supported sandbox target's `check` verifies the config classes and their
references in the distribution JAR, sources JAR, and development output. It
also checks that platform classes are not embedded and unselected UI classes
are absent. Build-logic contract tests cover transitive embedding, isolation
between consumers, source relocation, and configuration-cache reuse.

