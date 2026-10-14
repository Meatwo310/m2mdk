package net.meatwo310.m2mdk;

import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.GradleRunner;

/** Runs without a third-party test framework or Minecraft compilation. */
public final class PluginSmokeTest {
    public static void main(String[] args) throws Exception {
        Path fixture = Files.createTempDirectory(Path.of(".").toAbsolutePath(), "multi-project-");
        write(fixture.resolve("settings.gradle"), "rootProject.name = 'fixture'\ninclude 'alpha', 'beta'\n");
        for (String name : new String[] {"alpha", "beta"}) {
            write(fixture.resolve(name + "/build.gradle"), """
                    plugins { id 'net.meatwo310.m2mdk.neoforge' }
                    version = '%s-version'
                    m2mdk {
                        modId = '%s_mod'
                        modName = '%s display'
                    }
                    neoForge { version = '21.1.259' }
                    assert java.toolchain.languageVersion.get().asInt() == 21
                    assert tasks.findByName('runClient') != null
                    assert tasks.findByName('runServer') != null
                    """.formatted(name, name, name));
            write(fixture.resolve(name + "/src/main/templates/META-INF/neoforge.mods.toml"), """
                    modId="${mod_id}"
                    displayName="${mod_name}"
                    license="${mod_license}"
                    version="${mod_version}"
                    """);
        }
        var runner = GradleRunner.create().withProjectDir(fixture.toFile()).withPluginClasspath()
                .withArguments(":alpha:generateModMetadata", ":beta:generateModMetadata", "--configuration-cache", "--stacktrace");
        runner.build();
        for (String name : new String[] {"alpha", "beta"}) {
            String actual = Files.readString(fixture.resolve(name + "/build/generated/modMetadata/META-INF/neoforge.mods.toml"));
            require(actual.contains("modId=\"" + name + "_mod\""), "mod ID leaked between projects");
            require(actual.contains("displayName=\"" + name + " display\""), "display name did not expand");
            require(actual.contains("version=\"" + name + "-version\""), "project version did not expand");
            require(actual.contains("license=\"All Rights Reserved\""), "default license did not expand");
            require(!actual.contains("${"), "unexpanded template token");
        }
        require(runner.build().getOutput().contains("Reusing configuration cache."), "configuration cache was not reused");
        Path alpha = fixture.resolve("alpha/build.gradle");
        write(alpha, Files.readString(alpha).replace("alpha_mod", "INVALID-ID"));
        require(runner.buildAndFail().getOutput().contains("m2mdk.modId must match"), "invalid mod ID was not rejected");
        System.out.println("Passed: subproject metadata isolation, defaults, run tasks, configuration cache, invalid mod ID.");
    }

    private static void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
