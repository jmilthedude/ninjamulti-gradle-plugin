package net.ninjadev.ninjamulti;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

import java.io.File;

public class NinjaMultiSettingsPlugin implements Plugin<Settings> {

    @Override
    public void apply(Settings settings) {
        settings.pluginManagement(pm -> {
            pm.repositories(repos -> {
                repos.gradlePluginPortal();
                repos.mavenCentral();
                repos.exclusiveContent(ec -> {
                    ec.forRepository(() -> repos.maven(m -> {
                        m.setName("Fabric");
                        m.setUrl("https://maven.fabricmc.net");
                    }));
                    ec.filter(f -> {
                        f.includeGroupAndSubgroups("net.fabricmc");
                        f.includeGroup("fabric-loom");
                    });
                });
                repos.exclusiveContent(ec -> {
                    ec.forRepository(() -> repos.maven(m -> {
                        m.setName("Sponge");
                        m.setUrl("https://repo.spongepowered.org/repository/maven-public");
                    }));
                    ec.filter(f -> f.includeGroupAndSubgroups("org.spongepowered"));
                });
                repos.exclusiveContent(ec -> {
                    ec.forRepository(() -> repos.maven(m -> {
                        m.setName("Forge");
                        m.setUrl("https://maven.minecraftforge.net");
                    }));
                    ec.filter(f -> f.includeGroupAndSubgroups("net.minecraftforge"));
                });
            });
        });

        settings.getPlugins().apply("org.gradle.toolchains.foojay-resolver-convention");

        File rootDir = settings.getRootDir();
        String[] subprojects = {"common", "fabric", "neoforge", "forge"};
        for (String sub : subprojects) {
            File subDir = new File(rootDir, sub);
            if (subDir.isDirectory()) {
                settings.include(sub);
            }
        }
    }
}
