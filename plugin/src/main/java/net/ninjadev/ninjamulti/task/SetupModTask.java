package net.ninjadev.ninjamulti.task;

import net.ninjadev.ninjamulti.extension.NinjaMultiExtension;
import net.ninjadev.ninjamulti.util.NamingConventions;
import net.ninjadev.ninjamulti.util.TemplateRenderer;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.api.tasks.options.Option;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@UntrackedTask(because = "Generates source files based on gradle.properties, not a build output")
public abstract class SetupModTask extends DefaultTask {

    private boolean force = false;

    @Option(option = "force", description = "Overwrite existing files")
    public void setForce(boolean force) {
        this.force = force;
    }

    @TaskAction
    public void setup() {
        NinjaMultiExtension ext = getProject().getExtensions().getByType(NinjaMultiExtension.class);

        String modId = getProject().findProperty("mod_id").toString();
        String modName = getProject().findProperty("mod_name").toString();
        String group = getProject().getGroup().toString();
        String modPackage = ext.resolveModPackage();
        String modClassName = ext.resolveModClassName();
        String packagePath = NamingConventions.packageToPath(modPackage);

        Map<String, String> vars = new HashMap<>();
        vars.put("modId", modId);
        vars.put("modName", modName);
        vars.put("group", group);
        vars.put("modPackage", modPackage);
        vars.put("modClassName", modClassName);
        vars.put("packagePath", packagePath);

        File rootDir = getProject().getRootDir();

        getLogger().lifecycle("NinjaMulti: Generating mod scaffold for '{}' ({})", modName, modId);
        getLogger().lifecycle("  Package: {}", modPackage);
        getLogger().lifecycle("  Class name: {}", modClassName);

        generateIfSubprojectExists(rootDir, "common", "templates/common/Constants.java.template",
                "common/src/main/java/" + packagePath + "/Constants.java", vars);
        generateIfSubprojectExists(rootDir, "common", "templates/common/ModCommon.java.template",
                "common/src/main/java/" + packagePath + "/" + modClassName + "Common.java", vars);
        generateIfSubprojectExists(rootDir, "common", "templates/common/pack.mcmeta.template",
                "common/src/main/resources/pack.mcmeta", vars);
        generateIfSubprojectExists(rootDir, "common", "templates/universal/mixins.json.template",
                "common/src/main/resources/" + modId + ".mixins.json", vars);

        generateIfSubprojectExists(rootDir, "fabric", "templates/fabric/ModFabric.java.template",
                "fabric/src/main/java/" + packagePath + "/" + modClassName + "Fabric.java", vars);

        generateIfSubprojectExists(rootDir, "forge", "templates/forge/ModForge.java.template",
                "forge/src/main/java/" + packagePath + "/" + modClassName + "Forge.java", vars);

        generateIfSubprojectExists(rootDir, "neoforge", "templates/neoforge/ModNeoforge.java.template",
                "neoforge/src/main/java/" + packagePath + "/" + modClassName + "Neoforge.java", vars);

        // Universal metadata
        generate(rootDir, "templates/universal/fabric.mod.json.template",
                "universal/fabric.mod.json", vars);
        generate(rootDir, "templates/universal/mods.toml.template",
                "universal/META-INF/mods.toml", vars);
        generate(rootDir, "templates/universal/neoforge.mods.toml.template",
                "universal/META-INF/neoforge.mods.toml", vars);
        generate(rootDir, "templates/universal/mixins.json.template",
                "universal/" + modId + ".mixins.json", vars);
        generate(rootDir, "templates/universal/services.template",
                "universal/META-INF/services/net.ninjadev.ninjamulti.api.services.IPlatformHelper", vars);

        getLogger().lifecycle("NinjaMulti: Scaffold generation complete!");
    }

    private void generateIfSubprojectExists(File rootDir, String subproject, String template, String output, Map<String, String> vars) {
        File subDir = new File(rootDir, subproject);
        if (subDir.isDirectory()) {
            generate(rootDir, template, output, vars);
        }
    }

    private void generate(File rootDir, String template, String outputPath, Map<String, String> vars) {
        File outputFile = new File(rootDir, outputPath);
        if (outputFile.exists() && !force) {
            getLogger().lifecycle("  SKIP (exists): {}", outputPath);
            return;
        }
        TemplateRenderer.renderToFile(template, vars, outputFile, force);
        getLogger().lifecycle("  CREATED: {}", outputPath);
    }
}
