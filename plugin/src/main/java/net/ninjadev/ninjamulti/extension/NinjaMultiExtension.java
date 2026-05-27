package net.ninjadev.ninjamulti.extension;

import net.ninjadev.ninjamulti.util.NamingConventions;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public abstract class NinjaMultiExtension {

    private final Project project;

    public NinjaMultiExtension(Project project) {
        this.project = project;
    }

    public abstract Property<String> getModPackage();
    public abstract Property<String> getModClassName();
    public abstract Property<String> getBaseLoader();

    public String resolveModPackage() {
        if (getModPackage().isPresent()) {
            return getModPackage().get();
        }
        String group = project.getGroup().toString();
        String modId = project.findProperty("mod_id").toString();
        return NamingConventions.derivePackage(group, modId);
    }

    public String resolveModClassName() {
        if (getModClassName().isPresent()) {
            return getModClassName().get();
        }
        String modName = project.findProperty("mod_name").toString();
        return NamingConventions.toPascalCase(modName);
    }

    public String resolveBaseLoader() {
        if (getBaseLoader().isPresent()) {
            return getBaseLoader().get();
        }
        for (String loader : java.util.List.of("forge", "neoforge", "fabric")) {
            if (project.findProject(":" + loader) != null) {
                return loader;
            }
        }
        return "forge";
    }
}
