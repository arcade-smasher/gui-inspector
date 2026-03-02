package com.arcadesmasher.guiinspector.mappings;

import com.arcadesmasher.guiinspector.GUIInspector;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

public class MethodMappings {
    private static final Properties MAPPINGS = new Properties();

    static {
        try (InputStream is = MethodMappings.class.getResourceAsStream("/method_mappings.properties")) {
            if (is != null) {
                MAPPINGS.load(is);
            }
        } catch (Exception e) {
            GUIInspector.LOGGER.warn("Failed to load method mappings", e);
        }
    }

    public static String getMappedName(Method method) { // unused, might use later
        if (method == null) return "null";
        return getMappedName(method.getDeclaringClass(), method.getName());
    }

    public static String getMappedName(Class<?> clazz, String methodName) {
        if (clazz == null || methodName == null) return "null";

        String key = clazz.getName() + "#" + methodName;
        String mapped = MAPPINGS.getProperty(key);
        if (mapped != null) return mapped;

        // for inner classes, try enclosing classes
        Class<?> enclosing = clazz.getEnclosingClass();
        while (enclosing != null) {
            key = enclosing.getName() + "#" + methodName;
            mapped = MAPPINGS.getProperty(key);
            if (mapped != null) return mapped + " (inner)";
            enclosing = enclosing.getEnclosingClass();
        }

        return methodName; // fallback
    }

    public static String getMappedName(String fullyQualifiedMethod) {
        if (fullyQualifiedMethod == null) return "null";

        String mapped = MAPPINGS.getProperty(fullyQualifiedMethod);
        if (mapped != null) return mapped;

        // split at last # to separate class and method
        int hashIndex = fullyQualifiedMethod.lastIndexOf('#');
        if (hashIndex != -1) {
            String className = fullyQualifiedMethod.substring(0, hashIndex);
            String methodName = fullyQualifiedMethod.substring(hashIndex + 1);
            mapped = getMappedName(className, methodName);
            if (mapped != null) return mapped;
        }

        return fullyQualifiedMethod; // fallback
    }

    private static String getMappedName(String className, String methodName) {
        String key = className + "#" + methodName;
        String mapped = MAPPINGS.getProperty(key);
        if (mapped != null) return mapped;

        // try inner classes
        int lastDollar = className.lastIndexOf('$');
        while (lastDollar != -1) {
            String candidateClass = className.substring(0, lastDollar);
            key = candidateClass + "#" + methodName;
            mapped = MAPPINGS.getProperty(key);
            if (mapped != null) return mapped + " (inner)";
            lastDollar = candidateClass.lastIndexOf('$');
        }

        return methodName; // fallback
    }
}