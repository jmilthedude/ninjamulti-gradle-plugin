package net.ninjadev.ninjamulti;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.bundling.Jar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NinjaMultiCommonPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java-library");
        project.getPluginManager().apply("maven-publish");

        String modId = prop(project, "mod_id");
        String modName = prop(project, "mod_name");
        String modAuthor = prop(project, "mod_author");
        String mcVersion = prop(project, "minecraft_version");
        String javaVersion = prop(project, "java_version");

        project.getExtensions().getByType(org.gradle.api.plugins.BasePluginExtension.class)
                .getArchivesName().set(modId + "-" + project.getName() + "-" + mcVersion);

        project.getExtensions().getByType(org.gradle.api.plugins.JavaPluginExtension.class)
                .getToolchain().getLanguageVersion().set(
                        org.gradle.jvm.toolchain.JavaLanguageVersion.of(Integer.parseInt(javaVersion)));
        project.getExtensions().getByType(org.gradle.api.plugins.JavaPluginExtension.class)
                .withSourcesJar();
        project.getExtensions().getByType(org.gradle.api.plugins.JavaPluginExtension.class)
                .withJavadocJar();

        project.getRepositories().mavenCentral();
        project.getRepositories().exclusiveContent(ec -> {
            ec.forRepository(() -> project.getRepositories().maven(m -> {
                m.setName("Sponge");
                m.setUrl("https://repo.spongepowered.org/repository/maven-public");
            }));
            ec.filter(f -> f.includeGroupAndSubgroups("org.spongepowered"));
        });
        project.getRepositories().exclusiveContent(ec -> {
            ec.forRepositories(
                    project.getRepositories().maven(m -> {
                        m.setName("ParchmentMC");
                        m.setUrl("https://maven.parchmentmc.org/");
                    }),
                    project.getRepositories().maven(m -> {
                        m.setName("NeoForge");
                        m.setUrl("https://maven.neoforged.net/releases");
                    })
            );
            ec.filter(f -> f.includeGroup("org.parchmentmc.data"));
        });
        project.getRepositories().maven(m -> {
            m.setName("BlameJared");
            m.setUrl("https://maven.blamejared.com");
        });

        String archivesName = modId + "-" + project.getName() + "-" + mcVersion;
        List<String> variants = List.of("apiElements", "runtimeElements", "sourcesElements", "javadocElements");
        for (String variant : variants) {
            project.getConfigurations().getByName(variant).getOutgoing().capability(
                    project.getGroup() + ":" + archivesName + ":" + project.getVersion());
            project.getConfigurations().getByName(variant).getOutgoing().capability(
                    project.getGroup() + ":" + modId + "-" + project.getName() + "-" + mcVersion + ":" + project.getVersion());
        }

        project.getTasks().named("sourcesJar", Jar.class, jar -> {
            jar.from(project.getRootProject().file("LICENSE"), spec -> {
                spec.rename(s -> s + "_" + modName);
            });
        });

        project.getTasks().named("jar", Jar.class, jar -> {
            jar.from(project.getRootProject().file("LICENSE"), spec -> {
                spec.rename(s -> s + "_" + modName);
            });
            jar.getManifest().attributes(Map.of(
                    "Specification-Title", modName,
                    "Specification-Vendor", modAuthor,
                    "Specification-Version", jar.getArchiveVersion().get(),
                    "Implementation-Title", project.getName(),
                    "Implementation-Version", jar.getArchiveVersion().get(),
                    "Implementation-Vendor", modAuthor,
                    "Built-On-Minecraft", mcVersion
            ));
        });

        project.getTasks().named("processResources", task -> {
            Map<String, Object> expandProps = buildExpandProps(project);
            task.getInputs().properties(expandProps);
            ((org.gradle.language.jvm.tasks.ProcessResources) task).filesMatching(
                    List.of("pack.mcmeta", "fabric.mod.json", "META-INF/mods.toml",
                            "META-INF/neoforge.mods.toml", "*.mixins.json"),
                    details -> details.expand(expandProps)
            );
        });

        project.getExtensions().configure(PublishingExtension.class, publishing -> {
            publishing.publications(pubs -> {
                pubs.register("mavenJava", MavenPublication.class, pub -> {
                    pub.setArtifactId(archivesName);
                    pub.from(project.getComponents().getByName("java"));
                });
            });
            publishing.repositories(repos -> {
                repos.maven(m -> {
                    String localMavenUrl = System.getenv("local_maven_url");
                    if (localMavenUrl != null) {
                        m.setUrl(localMavenUrl);
                    }
                });
            });
        });

        PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
        for (String variant : variants) {
            publishing.getPublications().configureEach(pub -> {
                if (pub instanceof MavenPublication) {
                    ((MavenPublication) pub).suppressPomMetadataWarningsFor(variant);
                }
            });
        }

        if (project.getName().equals("common")) {
            project.getConfigurations().register("commonJava", conf -> {
                conf.setCanBeResolved(false);
                conf.setCanBeConsumed(true);
            });
            project.getConfigurations().register("commonResources", conf -> {
                conf.setCanBeResolved(false);
                conf.setCanBeConsumed(true);
            });

            project.afterEvaluate(p -> {
                org.gradle.api.plugins.JavaPluginExtension javaExt =
                        p.getExtensions().getByType(org.gradle.api.plugins.JavaPluginExtension.class);
                java.io.File javaSrcDir = javaExt.getSourceSets().getByName("main")
                        .getJava().getSourceDirectories().getSingleFile();
                java.io.File resSrcDir = javaExt.getSourceSets().getByName("main")
                        .getResources().getSourceDirectories().getSingleFile();
                p.getArtifacts().add("commonJava", javaSrcDir);
                p.getArtifacts().add("commonResources", resSrcDir);
            });
        }
    }

    static Map<String, Object> buildExpandProps(Project project) {
        Map<String, Object> props = new HashMap<>();
        props.put("version", project.getVersion());
        props.put("group", project.getGroup());
        props.put("minecraft_version", prop(project, "minecraft_version"));
        props.put("minecraft_version_range", prop(project, "minecraft_version_range"));
        props.put("fabric_version", prop(project, "fabric_version"));
        props.put("fabric_loader_version", prop(project, "fabric_loader_version"));
        props.put("mod_name", prop(project, "mod_name"));
        props.put("mod_author", prop(project, "mod_author"));
        props.put("mod_id", prop(project, "mod_id"));
        props.put("license", prop(project, "license"));
        props.put("description", project.getDescription() != null ? project.getDescription() : "");
        props.put("neoforge_version", prop(project, "neoforge_version"));
        props.put("neoforge_loader_version_range", prop(project, "neoforge_loader_version_range"));
        props.put("forge_version", prop(project, "forge_version"));
        props.put("forge_loader_version_range", prop(project, "forge_loader_version_range"));
        props.put("credits", prop(project, "credits"));
        props.put("java_version", prop(project, "java_version"));
        return props;
    }

    private static String prop(Project project, String name) {
        Object val = project.findProperty(name);
        return val != null ? val.toString() : "";
    }
}
