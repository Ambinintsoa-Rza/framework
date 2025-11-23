package com.monframework;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.monframework.annotations.Controller;
import com.monframework.annotations.UrlMapping;
import com.monframework.model.ModelView;

public class FrontServlet extends HttpServlet {

    // Map qui relie une URL à sa méthode et son contrôleur
    private Map<String, MethodMapping> mappings = new HashMap<>();

    // Petite classe interne pour stocker le contrôleur et la méthode
    private static class MethodMapping {
        Class<?> controllerClass;
        Method method;

        MethodMapping(Class<?> controllerClass, Method method) {
            this.controllerClass = controllerClass;
            this.method = method;
        }
    }

    //appelé une seule fois
    @Override
    public void init() throws ServletException {
        try {
            List<Class<?>> controllers = getControllers("com.test");

            for (Class<?> cls : controllers) {
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(UrlMapping.class)) {
                        UrlMapping mapping = m.getAnnotation(UrlMapping.class);
                        mappings.put(mapping.value(), new MethodMapping(cls, m));
                    }
                }
            }

            System.out.println("=== MAPPINGS DETECTÉS ===");
            for (Map.Entry<String, MethodMapping> entry : mappings.entrySet()) {
                System.out.println(entry.getKey() + " -> "
                        + entry.getValue().controllerClass.getName() + "."
                        + entry.getValue().method.getName());
            }
            System.out.println("===========================");

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

@Override
protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

    String uri = req.getRequestURI();
    String context = req.getContextPath();
    String relativePath = uri.substring(context.length());

    resp.setContentType("text/plain;charset=UTF-8");
    PrintWriter out = resp.getWriter();

    try {
        MethodMapping mapping = mappings.get(relativePath);

        if (mapping != null) {
            out.println("=== ROUTE TROUVÉE ===");
            out.println("Controller : " + mapping.controllerClass.getName());
            out.println("Méthode    : " + mapping.method.getName());
            out.println("=======================");

            Object controllerInstance = mapping.controllerClass.getDeclaredConstructor().newInstance();
            Object result = mapping.method.invoke(controllerInstance);

            // ================================
            //      GESTION DES RETOURS
            // ================================

            if (result instanceof String) {
                out.println(result);

            } else if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;

                // Mettre les données dans la requête
                for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }

                // Forward vers JSP
                RequestDispatcher dispatcher =
                        req.getRequestDispatcher("/" + mv.getView());
                dispatcher.forward(req, resp);
                return;

            } else {
                out.println("Type de retour non supporté : "
                        + (result != null ? result.getClass() : "null"));
            }

        } else {
            out.println("Aucune méthode trouvée pour : " + relativePath);
        }

    } catch (Exception e) {
        e.printStackTrace(out);
    }
}


    //Recherche des classes annotées @Controller dans un package donné.
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
    
    //Parcours récursif du répertoire pour trouver les classes @Controller.
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
