package net.ninjadev.ninjamulti;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;

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
    }
}
