package com.monframework;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.monframework.annotations.Controller;
import com.monframework.annotations.GetMapping;
import com.monframework.annotations.Param;
import com.monframework.annotations.PostMapping;
import com.monframework.annotations.UrlMapping;
import com.monframework.model.ModelView;

public class FrontServlet extends HttpServlet {

    // Map qui relie une URL à sa méthode et son contrôleur
    private Map<String, MethodMapping> mappings = new HashMap<>();

    // Petite classe interne pour stocker le contrôleur et la méthode
    private static class MethodMapping {
        Class<?> controllerClass;
        Method method;
        String httpMethod; // GET ou POST
        Pattern regex;
        List<String> variables;

        MethodMapping(Class<?> c, Method m, String httpMethod, Pattern regex, List<String> vars) {
            this.controllerClass = c;
            this.method = m;
            this.httpMethod = httpMethod;
            this.regex = regex;
            this.variables = vars;
        }
    }


private MethodMapping buildMapping(Class<?> cls, Method m, String url, String httpMethod) {
    List<String> variables = new ArrayList<>();

    Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(url);
    String regex = url;

    while (matcher.find()) {
        String varName = matcher.group(1);
        variables.add(varName);
        regex = regex.replace("{" + varName + "}", "([^/]+)");
    }

    Pattern pattern = Pattern.compile("^" + regex + "$");

    return new MethodMapping(cls, m, httpMethod, pattern, variables);
}


    private Object convert(String value, Class<?> type) {
    if (value == null) return null;
    if (type == String.class) return value;
    if (type == int.class || type == Integer.class) return Integer.parseInt(value);
    if (type == long.class || type == Long.class) return Long.parseLong(value);
    if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
    if (type == double.class || type == Double.class) return Double.parseDouble(value);
    return value; // fallback
}



    //appelé une seule fois
    @Override
    public void init() throws ServletException {
        try {
            List<Class<?>> controllers = getControllers("com.test");

            for (Class<?> cls : controllers) {
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(GetMapping.class)) {
                        String url = m.getAnnotation(GetMapping.class).value();
                        MethodMapping mm = buildMapping(cls, m, url, "GET");
                        mappings.put(url + "_GET", mm);
                    }

                    if (m.isAnnotationPresent(PostMapping.class)) {
                        String url = m.getAnnotation(PostMapping.class).value();
                        MethodMapping mm = buildMapping(cls, m, url, "POST");
                        mappings.put(url + "_POST", mm);
                    }

                    if (m.isAnnotationPresent(UrlMapping.class)) {
                        String url = m.getAnnotation(UrlMapping.class).value();
                        MethodMapping mm = buildMapping(cls, m, url, "ANY");
                        mappings.put(url + "_ANY", mm);
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
        MethodMapping mapping = null;
        Matcher matched = null;

        String httpMethod = req.getMethod(); // GET ou POST

        for (MethodMapping mm : mappings.values()) {
            Matcher mat = mm.regex.matcher(relativePath);
            if (mat.matches()) {

                // vérification GET/POST/ANY
                if (!mm.httpMethod.equals("ANY") && !mm.httpMethod.equals(httpMethod)) {
                    continue; // méthode HTTP incorrecte → on skip
                }
                mapping = mm;
                matched = mat;
                break;
            }
        }


        if (mapping != null) {
            out.println("=== ROUTE TROUVÉE ===");
            out.println("Controller : " + mapping.controllerClass.getName());
            out.println("Méthode    : " + mapping.method.getName());
            out.println("=======================");

            Object controllerInstance = mapping.controllerClass.getDeclaredConstructor().newInstance();

            // ================================
            //        EXTRACTION ARGUMENTS
            // ================================
        Object[] args = new Object[mapping.method.getParameterCount()];
        Parameter[] params = mapping.method.getParameters();

        for (int i = 0; i < params.length; i++) {
            Class<?> paramType = params[i].getType();
            
            // === Sprint 8 : Support Map<String, Object> ===
            if (Map.class.isAssignableFrom(paramType)) {
                // Créer une Map avec tous les paramètres de la requête
                Map<String, Object> allParams = new HashMap<>();
                
                // Ajouter les paramètres GET/POST classiques
                Map<String, String[]> requestParams = req.getParameterMap();
                for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
                    String[] values = entry.getValue();
                    if (values.length == 1) {
                        allParams.put(entry.getKey(), values[0]);
                    } else {
                        allParams.put(entry.getKey(), values);
                    }
                }
                
                // Ajouter aussi les variables de l'URL (path variables)
                for (int j = 0; j < mapping.variables.size(); j++) {
                    String varName = mapping.variables.get(j);
                    String varValue = matched.group(j + 1);
                    allParams.put(varName, varValue);
                }
                
                args[i] = allParams;
                continue;
            }
            
            String paramName = params[i].getName(); // si @Param absent
            Param p = params[i].getAnnotation(Param.class);
            if (p != null) paramName = p.value();

            int indexVar = mapping.variables.indexOf(paramName);

            if (indexVar >= 0) {
                String value = matched.group(indexVar + 1);
                args[i] = convert(value, params[i].getType());
            } else {
                // sinon = GET/POST param classique
                args[i] = convert(req.getParameter(paramName), params[i].getType());
            }
        }

            // Appel de la méthode AVEC arguments
            Object result = mapping.method.invoke(controllerInstance, args);


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
