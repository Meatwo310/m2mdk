# m2mdk

Reusable modules extracted from the multi-version Minecraft sandbox.

## Builds

- The repository-root Gradle build owns library subprojects under `packages/`.
  It does not include or configure Minecraft projects.
- `build-logic/` provides opt-in library convention plugins. Each module owns
  its dependencies, group, and version; dependencies between modules must be
  declared explicitly.
- `sandbox/` is a separate integration build. It uses `includeBuild("..")` to
  consume local library variants through normal module coordinates. Library
  code must not depend on sandbox sources, tasks, or loader plugins.

Use JDK 25 to run either wrapper. `nix develop` provides the development tools.
The library convention defaults to a Java 17 toolchain and matching bytecode,
UTF-8 compilation, a sources jar, and one test fork with a 256 MB heap. Modules
can override the Java toolchain when their requirements differ. Root Gradle
properties limit library builds to one worker with parallel execution disabled.

## Library development

Run from the repository root:

```sh
./gradlew build
./gradlew :config-core:build
```

Root `assemble`, `check`, `build`, and `clean` tasks aggregate the library
subprojects. Artifacts stay under each module's `build/libs/` directory.

To add a library, create `packages/<name>/build.gradle.kts`, apply
`mdk.java-library-conventions`, and declare its group, version, and dependencies.
Register it in root `settings.gradle.kts`:

```kotlin
include(":<name>")
project(":<name>").projectDir = file("packages/<name>")
```

Use explicit `project(":<name>")` dependencies within the library build. Sandbox
consumers use the published module coordinate, substituted by the composite
build during local development. Libraries are not published automatically.

## Minecraft integration

Run the sandbox wrapper separately, for example:

```sh
./sandbox/gradlew -p sandbox :26.1.2-fabric:build --no-parallel --max-workers=1
```

The sandbox has its own settings, wrapper, loader conventions, and Gradle
properties. See its [English guide](sandbox/docs/README.md) or
[Japanese guide](sandbox/docs/README.ja.md) for platform-specific workflows.
