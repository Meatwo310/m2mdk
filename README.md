# m2mdk

> [!WARNING]
> This project is experimental. Backward compatibility is not guaranteed.

m2mdk provides reusable Gradle plugins and libraries for
[m2mdk-template](https://github.com/Meatwo310/m2mdk-template), a modular Minecraft
mod development template that supports multiple Minecraft versions and mod loaders.

Reusable config APIs and screens live under `packages/`, organized by Minecraft
version, loader, and feature. The `sandbox/` build provides example Mods and
integration checks. Libraries are embedded into each consuming Mod with relocated
packages to avoid conflicts.

## Development

Use JDK 25, or enter the Nix development environment with `nix develop`.

Build the libraries for one Minecraft version:

```sh
./gradlew -Pmdk.minecraft=26.1.2 build
```

The default configures only 26.1.2. Use `-Pmdk.libraryMinecraft=all` explicitly
to build every supported library version. For a sandbox integration build:

```sh
CI=true ./sandbox/gradlew -p sandbox -Pmdk.minecraft=26.1.2 :26.1.2-fabric:build
```

`CI=true` avoids Minecraft decompilation in ModDevGradle; leave it unset when you
need Minecraft sources for debugging.

See [Gradle plugins](docs/gradle-plugins.md) for using the conventions without
copying `buildSrc`. See [Library development](docs/development.md) for module structure, embedding,
and verification, and [Contributing](CONTRIBUTING.md) for contribution guidelines.

## License

[MIT](LICENSE)
