# Config Core

Loader-neutral config declarations, entry builders, value access, and binding
contracts. This is an independent Java 17 Gradle build. Its main code depends
only on the JDK and compile-time JSpecify annotations; it does not depend on
Minecraft, a mod loader, a screen API, or the sandbox.

```sh
./gradlew build --no-parallel --max-workers=1
```

The wrapper can run on JDK 25, while the Java toolchain compiles and tests with
Java 17. The settings plugin can provision the Java 17 toolchain when needed.
Tests use an in-memory adapter to exercise definitions, binding, writes and
saves, metadata ordering, declaration extension, and compatibility aliases.

The library coordinate is `net.meatwo310.mdk:config-core:0.1.0-SNAPSHOT`. It is
not published yet. The sandbox consumes this coordinate through a composite
build in its settings, without reading this package's source directories or
reaching into its tasks. Both binary and sources variants are consumed through
normal Java library configurations.

The API namespace is `net.meatwo310.mdk.config`. Platform adapters implement
`ConfigEntryBinder.Adapter`; value backends supply `ConfigEntryBinding`.
Adapters, client screens, and editable example declarations remain in sandbox.
The sandbox embeds the library in its mod jar and development staging directory.

Source code was extracted from the MIT-licensed custom-mdk template; see
[LICENSE](LICENSE). The extraction changes the package namespace while retaining
the existing value, declaration, and binding behavior.
