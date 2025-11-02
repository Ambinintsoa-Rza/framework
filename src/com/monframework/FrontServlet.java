package com.monframework;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.monframework.annotations.Controller;
import com.monframework.annotations.UrlMapping;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String context = req.getContextPath();
        String relativePath = uri.substring(context.length());

        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            // 1. Lister tous les controlleurs detectes
            List<Class<?>> controllers = getControllers("com.test");

            out.println("=== Liste des classes avec @Controller detectees ===");
            if (controllers.isEmpty()) {
                out.println("(Aucun controleur trouve)");
            } else {
                for (Class<?> c : controllers) {
                    out.println("-> " + c.getName());
                }
            }
            out.println("========================================");
            out.println();

            // 2. Parcourir les controlleurs pour trouver la methode correspondant a l'URL
            boolean found = false;
            for (Class<?> cls : controllers) {
                Object controllerInstance = cls.getDeclaredConstructor().newInstance();

                for (Method m : cls.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(UrlMapping.class)) {
                        UrlMapping mapping = m.getAnnotation(UrlMapping.class);

                        if (mapping.value().equals(relativePath)) {
                            Object result = m.invoke(controllerInstance);
                            out.println("Resultat methode : " + result);
                            found = true;
                            break;
                        }
                    }
                }
                if (found) break;
            }

            if (!found) {
                out.println("Aucune methode trouvee pour : " + relativePath);
            }

        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }

    private List<Class<?>> getControllers(String basePackage) {
        List<Class<?>> controllers = new ArrayList<>();
        String path = getServletContext().getRealPath("/WEB-INF/classes/" + basePackage.replace('.', '/'));

        File directory = new File(path);
        if (!directory.exists()) {
            System.out.println("Dossier introuvable : " + path);
            return controllers;
        }

        scanDirectory(directory, basePackage, controllers);
        return controllers;
    }

    private void scanDirectory(File folder, String packageName, List<Class<?>> controllers) {
        for (File file : Objects.requireNonNull(folder.listFiles())) {
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
}
