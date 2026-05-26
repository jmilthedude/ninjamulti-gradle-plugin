package net.ninjadev.ninjamulti.util;

public class NamingConventions {

    public static String toPascalCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String derivePackage(String group, String modId) {
        return group + "." + modId;
    }

    public static String packageToPath(String pkg) {
        return pkg.replace('.', '/');
    }
}
