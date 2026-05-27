package net.ninjadev.ninjamulti;

import net.ninjadev.ninjamulti.extension.NinjaMultiExtension;
import net.ninjadev.ninjamulti.task.SetupModTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class NinjaMultiRootPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java-base");

        NinjaMultiExtension extension = project.getExtensions().create("ninjamulti", NinjaMultiExtension.class, project);

        project.getTasks().register("setupMod", SetupModTask.class, task -> {
            task.setGroup("ninjamulti");
            task.setDescription("Generates boilerplate source files for a new multi-loader mod");
        });

        project.afterEvaluate(p -> {
            registerUniversalJarTask(p, extension);
        });
    }

    private void registerUniversalJarTask(Project project, NinjaMultiExtension extension) {
        String modId = project.findProperty("mod_id") != null ? project.findProperty("mod_id").toString() : "mod";
        String modName = project.findProperty("mod_name") != null ? project.findProperty("mod_name").toString() : "Mod";
        String mcVersion = project.findProperty("minecraft_version") != null ? project.findProperty("minecraft_version").toString() : "";
        String modPackage = extension.resolveModPackage();
        String modPackagePath = modPackage.replace('.', '/');
        String baseLoader = extension.resolveBaseLoader();

        List<String> loaders = new ArrayList<>();
        for (String loader : List.of("fabric", "forge", "neoforge")) {
            if (project.findProject(":" + loader) != null) {
                loaders.add(loader);
            }
        }

        if (loaders.isEmpty()) return;

        List<String> dependsOnTasks = new ArrayList<>();
        for (String loader : loaders) {
            dependsOnTasks.add(":" + loader + ":jar");
        }

        project.getTasks().register("universalJar", Jar.class, jar -> {
            jar.setGroup("build");
            jar.setDescription("Creates a single JAR that works on all enabled loaders");
            jar.dependsOn(dependsOnTasks.toArray());

            jar.getArchiveFileName().set(modName + "-" + mcVersion + "-" + project.getVersion() + ".jar");
            jar.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("libs"));
            jar.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);

            Map<String, Set<String>> loaderSpecificPatterns = new HashMap<>();
            for (String loader : loaders) {
                Project loaderProject = project.project(":" + loader);
                Set<String> patterns = scanLoaderSources(loaderProject);
                loaderSpecificPatterns.put(loader, patterns);
            }

            if (!loaders.contains(baseLoader)) {
                return;
            }

            Project baseProject = project.project(":" + baseLoader);
            jar.from(project.zipTree(baseProject.getTasks().named("jar", Jar.class).get().getArchiveFile()), spec -> {
                spec.include(modPackagePath + "/**");
                spec.include("assets/**");
                spec.include("data/**");
                spec.include("pack.mcmeta");
                for (String loader : loaders) {
                    if (!loader.equals(baseLoader)) {
                        for (String pattern : loaderSpecificPatterns.get(loader)) {
                            spec.exclude(pattern);
                        }
                    }
                }
            });

            for (String loader : loaders) {
                if (!loader.equals(baseLoader)) {
                    Project loaderProject = project.project(":" + loader);
                    jar.from(project.zipTree(loaderProject.getTasks().named("jar", Jar.class).get().getArchiveFile()), spec -> {
                        for (String pattern : loaderSpecificPatterns.get(loader)) {
                            spec.include(pattern);
                        }
                    });
                }
            }

            Map<String, Object> expandProps = NinjaMultiCommonPlugin.buildExpandProps(project);
            jar.from("universal", spec -> {
                spec.filesMatching(List.of("fabric.mod.json", "META-INF/mods.toml",
                        "META-INF/neoforge.mods.toml", "*.mixins.json"), details -> {
                    details.expand(expandProps);
                });
            });

            jar.getManifest().attributes(Map.of(
                    "MixinConfigs", modId + ".mixins.json"
            ));
        });
    }

    private Set<String> scanLoaderSources(Project loaderProject) {
        Set<String> patterns = new LinkedHashSet<>();
        File srcDir = new File(loaderProject.getProjectDir(), "src/main/java");
        if (!srcDir.exists()) return patterns;

        collectJavaFiles(srcDir, srcDir, patterns);
        return patterns;
    }

    private void collectJavaFiles(File baseDir, File dir, Set<String> patterns) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                collectJavaFiles(baseDir, file, patterns);
            } else if (file.getName().endsWith(".java")) {
                String relativePath = baseDir.toPath().relativize(file.toPath()).toString()
                        .replace('\\', '/')
                        .replace(".java", "");
                patterns.add(relativePath + ".class");
                patterns.add(relativePath + "$*.class");
            }
        }
    }
}
