package com.monframework.util;

import com.monframework.annotations.Controller;
import com.monframework.annotations.GetMapping;
import com.monframework.annotations.PostMapping;
import com.monframework.annotations.UrlMapping;
import com.monframework.mapping.MethodMapping;

import jakarta.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Scanner pour détecter les contrôleurs et leurs mappings.
 */
public class ControllerScanner {

    private final ServletContext servletContext;

    public ControllerScanner(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Scanne un package et retourne tous les mappings détectés.
     */
    public Map<String, MethodMapping> scanPackage(String basePackage) {
        Map<String, MethodMapping> mappings = new HashMap<>();
        List<Class<?>> controllers = getControllers(basePackage);

        for (Class<?> cls : controllers) {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.isAnnotationPresent(GetMapping.class)) {
                    String url = m.getAnnotation(GetMapping.class).value();
                    MethodMapping mm = MethodMapping.build(cls, m, url, "GET");
                    mappings.put(url + "_GET", mm);
                }

                if (m.isAnnotationPresent(PostMapping.class)) {
                    String url = m.getAnnotation(PostMapping.class).value();
                    MethodMapping mm = MethodMapping.build(cls, m, url, "POST");
                    mappings.put(url + "_POST", mm);
                }

                if (m.isAnnotationPresent(UrlMapping.class)) {
                    String url = m.getAnnotation(UrlMapping.class).value();
                    MethodMapping mm = MethodMapping.build(cls, m, url, "ANY");
                    mappings.put(url + "_ANY", mm);
                }
            }
        }

        return mappings;
    }

    /**
     * Recherche des classes annotées @Controller dans un package donné.
     */
    private List<Class<?>> getControllers(String basePackage) {
        List<Class<?>> controllers = new ArrayList<>();
        String path = servletContext.getRealPath("/WEB-INF/classes/" + basePackage.replace('.', '/'));

        File directory = new File(path);
        if (!directory.exists()) {
            System.out.println("Dossier introuvable : " + path);
            return controllers;
        }

        scanDirectory(directory, basePackage, controllers);
        return controllers;
    }

    /**
     * Parcours récursif du répertoire pour trouver les classes @Controller.
     */
    private void scanDirectory(File folder, String packageName, List<Class<?>> controllers) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), controllers);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> cls = Class.forName(className);
                    if (cls.isAnnotationPresent(Controller.class)) {
                        controllers.add(cls);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Affiche les mappings détectés (debug).
     */
    public static void printMappings(Map<String, MethodMapping> mappings) {
        System.out.println("=== MAPPINGS DETECTÉS ===");
        for (Map.Entry<String, MethodMapping> entry : mappings.entrySet()) {
            System.out.println(entry.getKey() + " -> "
                    + entry.getValue().getControllerClass().getName() + "."
                    + entry.getValue().getMethod().getName());
        }
        System.out.println("===========================");
    }
}
