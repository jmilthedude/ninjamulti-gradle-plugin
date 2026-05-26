package net.ninjadev.ninjamulti.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TemplateRenderer {

    public static String render(String templatePath, Map<String, String> variables) {
        try (InputStream is = TemplateRenderer.class.getClassLoader().getResourceAsStream(templatePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Template not found: " + templatePath);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return content;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read template: " + templatePath, e);
        }
    }

    public static void renderToFile(String templatePath, Map<String, String> variables, File outputFile, boolean force) {
        if (outputFile.exists() && !force) {
            return;
        }
        outputFile.getParentFile().mkdirs();
        String content = render(templatePath, variables);
        try (Writer writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write file: " + outputFile, e);
        }
    }
}
