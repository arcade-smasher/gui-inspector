package com.arcadesmasher.guiinspector.mappings;

import com.arcadesmasher.guiinspector.GUIInspector;

import java.io.InputStream;
import java.util.Properties;

public class ClassMappings {
    private static final Properties MAPPINGS = new Properties();

    static {
        try (InputStream is = ClassMappings.class.getResourceAsStream("/class_mappings.properties")) {
            if (is != null) {
                MAPPINGS.load(is);
            }
        } catch (Exception e) {
            GUIInspector.LOGGER.warn("Failed to load class mappings", e);
        }
    }

    public static String getMappedName(Object object) {
        if (object == null) return "null";
        return getMappedName(object.getClass());
    }

    public static String getMappedName(Class<?> clazz) {
        String name = clazz.getName();
        // walk up until a mapping is found
        String mapped = MAPPINGS.getProperty(name);
        if (mapped != null) return mapped;

        // for anonymous classes, try the enclosing named class
        Class<?> enclosing = clazz.getEnclosingClass();
        while (enclosing != null) {
            mapped = MAPPINGS.getProperty(enclosing.getName());
            if (mapped != null) return mapped + " (anonymous)";
            enclosing = enclosing.getEnclosingClass();
        }

        return name; // fallback to default class name
    }

    public static String getMappedName(String className) {
        String mapped = MAPPINGS.getProperty(className);
        if (mapped != null) return mapped;

        // split at $ and walk backwards
        int lastDollar = className.lastIndexOf('$');
        while (lastDollar != -1) {
            String candidate = className.substring(0, lastDollar);
            mapped = MAPPINGS.getProperty(candidate);
            if (mapped != null) {
                String suffix = className.substring(lastDollar);
                if (suffix.matches("\\$\\d+")) {
                    return mapped + " (anonymous)";
                } else {
                    return mapped + suffix.replace('$', '.'); // show inner class suffix
                }
            }
            lastDollar = candidate.lastIndexOf('$');
        }

        return className; // fallback
    }
}