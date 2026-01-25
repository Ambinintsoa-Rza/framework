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
import com.monframework.annotations.Json;

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

    // Types simples gérés directement via convert()
    private boolean isSimpleType(Class<?> type) {
        return type == String.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == boolean.class || type == Boolean.class
                || type == double.class || type == Double.class;
    }

    // --- JSON helpers ---
    private String escapeJson(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return '"' + escapeJson((String)obj) + '"';
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);

        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Object eObj : ((Map<?,?>)obj).entrySet()) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) eObj;
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escapeJson(String.valueOf(e.getKey()))).append('"').append(':')
                  .append(toJson(e.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }

        if (obj instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (Object item : (Collection<?>)obj) {
                if (!first) sb.append(',');
                first = false;
                sb.append(toJson(item));
            }
            sb.append(']');
            return sb.toString();
        }

        if (obj.getClass().isArray()) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            int len = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                Object v = java.lang.reflect.Array.get(obj, i);
                sb.append(toJson(v));
            }
            sb.append(']');
            return sb.toString();
        }

        // Fallback POJO: sérialiser ses champs déclarés
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        Class<?> c = obj.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod)) continue;
                f.setAccessible(true);
                try {
                    Object v = f.get(obj);
                    if (!first) sb.append(',');
                    first = false;
                    sb.append('"').append(escapeJson(f.getName())).append('"').append(':')
                      .append(toJson(v));
                } catch (IllegalAccessException ignored) {}
            }
            c = c.getSuperclass();
        }
        sb.append('}');
        return sb.toString();
    }

    // Tentative de remplissage d'un champ d'objet depuis une valeur String
    private void trySetField(Object instance, String fieldName, String stringValue) {
        if (stringValue == null) return;
        Class<?> cls = instance.getClass();
        try {
            Field f = cls.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object converted = convert(stringValue, f.getType());
            f.set(instance, converted);
        } catch (NoSuchFieldException e) {
            // champ introuvable → on ignore
        } catch (Exception e) {
            // problème de conversion/affectation → on ignore pour ne pas casser la requête
        }
    }

    // Création et binding d'un objet paramètre à partir des paramètres requête + variables de chemin
    private Object bindObjectParam(HttpServletRequest req, Matcher matched, MethodMapping mapping, Class<?> paramType) throws Exception {
        Object instance = paramType.getDeclaredConstructor().newInstance();

        // 1) Variables de l'URL (path variables)
        for (int j = 0; j < mapping.variables.size(); j++) {
            String varName = mapping.variables.get(j);
            String varValue = matched.group(j + 1);
            trySetField(instance, varName, varValue);
        }

        // 2) Paramètres GET/POST classiques
        Map<String, String[]> requestParams = req.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            String val = (values != null && values.length > 0) ? values[0] : null;
            trySetField(instance, key, val);
        }

        return instance;
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
            boolean wantsJson = mapping.method.isAnnotationPresent(Json.class);
            if (!wantsJson) {
                out.println("=== ROUTE TROUVÉE ===");
                out.println("Controller : " + mapping.controllerClass.getName());
                out.println("Méthode    : " + mapping.method.getName());
                out.println("=======================");
            }

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

            // === Sprint 8-bis : Support des objets comme paramètres ===
            if (!isSimpleType(paramType)) {
                try {
                    args[i] = bindObjectParam(req, matched, mapping, paramType);
                    continue;
                } catch (Exception e) {
                    // si échec du binding, on continue vers la logique classique
                }
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

            if (mapping.method.isAnnotationPresent(Json.class)) {
                // JSON response
                resp.setContentType("application/json;charset=UTF-8");
                String payload;
                if (result instanceof ModelView) {
                    payload = toJson(((ModelView) result).getData());
                } else {
                    payload = toJson(result);
                }
                out.print(payload);

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
