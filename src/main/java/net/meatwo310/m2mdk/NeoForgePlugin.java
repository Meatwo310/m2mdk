package net.meatwo310.m2mdk;

import java.util.Map;
import net.neoforged.moddevgradle.dsl.NeoForgeExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

/** Project-local conventions; the native neoForge DSL remains available. */
public final class NeoForgePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("net.neoforged.moddev");

        var metadata = project.getExtensions().create("m2mdk", ModExtension.class);
        metadata.getModName().convention(metadata.getModId());
        metadata.getLicense().convention("All Rights Reserved");

        var java = project.getExtensions().getByType(JavaPluginExtension.class);
        java.getToolchain().getLanguageVersion().convention(JavaLanguageVersion.of(21));
        project.getTasks().withType(JavaCompile.class).configureEach(task ->
                task.getOptions().setEncoding("UTF-8"));

        var main = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        var neoForge = project.getExtensions().getByType(NeoForgeExtension.class);
        // This is a source-group name, independent of the mod ID in the TOML.
        neoForge.getMods().create("main", mod -> mod.sourceSet(main));
        neoForge.getRuns().create("client", run -> run.client());
        neoForge.getRuns().create("server", run -> {
            run.server();
            run.programArgument("--nogui");
        });

        var modId = metadata.getModId().map(value -> {
            if (!value.matches("[a-z][a-z0-9_]{1,63}")) {
                throw new IllegalArgumentException("m2mdk.modId must match [a-z][a-z0-9_]{1,63}: " + value);
            }
            return value;
        });
        var generateMetadata = project.getTasks().register("generateModMetadata", ProcessResources.class, task -> {
            var version = project.getVersion().toString();
            var properties = modId.zip(metadata.getModName(), (id, name) -> Map.of(
                    "mod_id", id, "mod_name", name)).zip(metadata.getLicense(), (values, license) -> Map.of(
                    "mod_id", values.get("mod_id"),
                    "mod_name", values.get("mod_name"),
                    "mod_license", license,
                    "mod_version", version));
            task.setDescription("Expands mod metadata templates from src/main/templates.");
            task.from(project.getLayout().getProjectDirectory().dir("src/main/templates"));
            task.into(project.getLayout().getBuildDirectory().dir("generated/modMetadata"));
            task.getInputs().property("modMetadata", properties);
            task.filesMatching("META-INF/neoforge.mods.toml", file -> file.expand(properties.get()));
        });
        main.getResources().srcDir(generateMetadata);
        neoForge.ideSyncTask(generateMetadata);
    }
}
