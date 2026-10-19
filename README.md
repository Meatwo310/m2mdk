# m2mdk

> [!WARNING]
> This project is experimental. Backward compatibility is not guaranteed.

m2mdk is a monorepo of reusable libraries for
[m2mdk-template](https://github.com/Meatwo310/m2mdk-template), a modular Minecraft
mod development template that supports multiple Minecraft versions and mod loaders.

Building on the experience of
[custom-mdk](https://github.com/Meatwo310/custom-mdk), m2mdk moves shared functionality
into reusable modules to reduce boilerplate and make the template simpler and more
flexible.

## Development

Use JDK 25 to run Gradle. The Foojay Toolchain Resolver downloads additional JDKs
as needed. If you use Nix, run `nix develop` to enter the development environment.

Build and test the libraries from the repository root:

```sh
./gradlew build
```

Library modules live under `packages/`. The `sandbox/` directory is a separate
Minecraft integration build that consumes the local libraries. To build a sandbox
target, run:

```sh
./sandbox/gradlew -p sandbox :26.1.2-fabric:build
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for commit message and scope conventions.

## License

[MIT](LICENSE)
