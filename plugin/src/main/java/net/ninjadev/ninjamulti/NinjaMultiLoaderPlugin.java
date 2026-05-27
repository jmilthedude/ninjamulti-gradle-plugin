package net.ninjadev.ninjamulti;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;

import java.util.Map;

public class NinjaMultiLoaderPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(NinjaMultiCommonPlugin.class);

        project.getConfigurations().register("commonJava", conf -> {
            conf.setCanBeResolved(true);
            conf.setCanBeConsumed(false);
        });

        project.getConfigurations().register("commonResources", conf -> {
            conf.setCanBeResolved(true);
            conf.setCanBeConsumed(false);
        });

        String modId = project.findProperty("mod_id") != null ? project.findProperty("mod_id").toString() : "";

        project.getDependencies().add("compileOnly",
                project.getDependencies().project(java.util.Map.of(
                        "path", ":common",
                        "configuration", "default"
                )));

        project.getDependencies().add("commonJava",
                project.getDependencies().project(java.util.Map.of(
                        "path", ":common",
                        "configuration", "commonJava"
                )));

        project.getDependencies().add("commonResources",
                project.getDependencies().project(java.util.Map.of(
                        "path", ":common",
                        "configuration", "commonResources"
                )));

        project.getTasks().named("compileJava", JavaCompile.class, task -> {
            task.dependsOn(project.getConfigurations().getByName("commonJava"));
            task.source(project.getConfigurations().getByName("commonJava"));
        });

        project.getTasks().named("processResources", task -> {
            task.dependsOn(project.getConfigurations().getByName("commonResources"));
            ((org.gradle.language.jvm.tasks.ProcessResources) task)
                    .from(project.getConfigurations().getByName("commonResources"));
        });

        project.getTasks().named("javadoc", Javadoc.class, task -> {
            task.dependsOn(project.getConfigurations().getByName("commonJava"));
            task.source(project.getConfigurations().getByName("commonJava"));
        });

        project.getTasks().named("sourcesJar", Jar.class, task -> {
            task.dependsOn(project.getConfigurations().getByName("commonJava"));
            task.from(project.getConfigurations().getByName("commonJava"));
            task.dependsOn(project.getConfigurations().getByName("commonResources"));
            task.from(project.getConfigurations().getByName("commonResources"));
        });

        project.getTasks().named("jar", Jar.class, jar -> {
            jar.getManifest().attributes(Map.of("MixinConfigs", modId + ".mixins.json"));
        });

        String projectName = project.getName();
        if (projectName.equals("forge") || projectName.equals("neoforge")) {
            project.afterEvaluate(p -> {
                JavaPluginExtension javaExt = p.getExtensions().getByType(JavaPluginExtension.class);
                javaExt.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                        .getResources().srcDir("src/generated/resources");
            });
        }

        if (projectName.equals("forge")) {
            project.afterEvaluate(p -> {
                JavaPluginExtension javaExt = p.getExtensions().getByType(JavaPluginExtension.class);
                javaExt.getSourceSets().all(ss -> {
                    ss.getOutput().setResourcesDir(
                            p.getLayout().getBuildDirectory().dir("sourcesSets/" + ss.getName()));
                    ss.getJava().getDestinationDirectory().set(
                            p.getLayout().getBuildDirectory().dir("sourcesSets/" + ss.getName()));
                });
            });
        }
    }
}
