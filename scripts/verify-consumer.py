#!/usr/bin/env python3
"""Inspect a consumer's declared release jars without imposing sample names."""
import argparse
import json
from pathlib import Path
import tomllib
import zipfile


def verify(root: Path, *, config: bool, ui: bool) -> None:
    """Check identity, embedded features, sources and archive integrity."""
    props = dict(
        line.split("=", 1) for line in (root / "gradle.properties").read_text().splitlines()
        if "=" in line and not line.lstrip().startswith("#")
    )
    version = (root / "version.txt").read_text().strip()
    jars = sorted(
        jar for jar in (root / "build/release").glob("*/*.jar")
        if not jar.name.endswith("-sources.jar")
    )
    assert jars, "assembleRelease produced no distributable jars"
    for binary in jars:
        with zipfile.ZipFile(binary) as jar:
            names = jar.namelist()
            assert len(names) == len(set(names)), f"Duplicate entries: {binary}"
            assert f"META-INF/{props['modId']}/LICENSE" in names
            assert not any(name.startswith("net/meatwo310/mdk/") for name in names)
            assert any(name.endswith("/config/ConfigDeclaration.class") for name in names) == config
            assert any("/config/ui/" in name and name.endswith(".class") for name in names) == ui
            assert any(name.endswith(".class") for name in names)
            if "fabric.mod.json" in names:
                metadata = json.loads(jar.read("fabric.mod.json"))
                assert metadata["id"] == props["modId"]
                assert metadata["version"] == version
                assert ("forgeconfigapiport" in metadata["depends"]) == config
                assert "META-INF/neoforge.mods.toml" not in names
            else:
                metadata = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode())
                assert metadata["mods"][0]["modId"] == props["modId"]
                assert metadata["mods"][0]["version"] == version
            if config:
                assert "META-INF/m2mdk/LICENSE" in names
        sources = binary.with_name(binary.stem + "-sources.jar")
        with zipfile.ZipFile(sources) as jar:
            assert any(name.endswith(".java") for name in jar.namelist())
            assert not any(name.startswith("net/meatwo310/mdk/") for name in jar.namelist())
        print(f"PASS {binary.name}: identity, sources, Config={config}, UI={ui}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("consumer", type=Path)
    parser.add_argument("--without-config", action="store_true")
    parser.add_argument("--without-ui", action="store_true")
    args = parser.parse_args()
    verify(args.consumer.resolve(), config=not args.without_config, ui=not (args.without_config or args.without_ui))
