package com.monframework.handler;

import com.monframework.annotations.Param;
import com.monframework.annotations.Session;
import com.monframework.mapping.MethodMapping;
import com.monframework.model.UploadedFile;
import com.monframework.util.TypeConverter;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Résolution des arguments pour les méthodes de contrôleur.
 */
public class ArgumentResolver {

    /**
     * Résout tous les arguments d'une méthode à partir de la requête.
     */
    public static Object[] resolveArguments(HttpServletRequest req, MethodMapping mapping, Matcher matched) throws Exception {
        Parameter[] params = mapping.getMethod().getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            args[i] = resolveArgument(req, mapping, matched, params[i]);
        }

        return args;
    }

    /**
     * Résout un argument individuel.
     */
    private static Object resolveArgument(HttpServletRequest req, MethodMapping mapping, 
                                          Matcher matched, Parameter param) throws Exception {
        Class<?> paramType = param.getType();

        // @Session Map<String, Object>
        if (Map.class.isAssignableFrom(paramType) && param.isAnnotationPresent(Session.class)) {
            return SessionHandler.loadSession(req);
        }

        // Map<String, Object> pour les paramètres
        if (Map.class.isAssignableFrom(paramType)) {
            return resolveMapParam(req, mapping, matched);
        }

        // UploadedFile
        if (UploadedFile.class.isAssignableFrom(paramType)) {
            String paramName = getParamName(param);
            return FileUploadHandler.getUploadedFile(req, paramName);
        }

        // Objet complexe
        if (!TypeConverter.isSimpleType(paramType)) {
            try {
                return bindObjectParam(req, matched, mapping, paramType);
            } catch (Exception e) {
                // Fallback vers la logique classique
            }
        }

        // Type simple
        return resolveSimpleParam(req, mapping, matched, param);
    }

    /**
     * Résout un paramètre de type Map avec tous les paramètres de la requête.
     */
    private static Map<String, Object> resolveMapParam(HttpServletRequest req, 
                                                        MethodMapping mapping, Matcher matched) {
        Map<String, Object> allParams = new HashMap<>();

        // Paramètres GET/POST
        Map<String, String[]> requestParams = req.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String[] values = entry.getValue();
            if (values.length == 1) {
                allParams.put(entry.getKey(), values[0]);
            } else {
                allParams.put(entry.getKey(), values);
            }
        }

        // Variables de l'URL
        for (int j = 0; j < mapping.getVariables().size(); j++) {
            String varName = mapping.getVariables().get(j);
            String varValue = matched.group(j + 1);
            allParams.put(varName, varValue);
        }

        return allParams;
    }

    /**
     * Résout un paramètre simple (String, int, etc.).
     */
    private static Object resolveSimpleParam(HttpServletRequest req, MethodMapping mapping,
                                             Matcher matched, Parameter param) {
        String paramName = getParamName(param);
        int indexVar = mapping.getVariables().indexOf(paramName);

        if (indexVar >= 0) {
            String value = matched.group(indexVar + 1);
            return TypeConverter.convert(value, param.getType());
        } else {
            return TypeConverter.convert(req.getParameter(paramName), param.getType());
        }
    }

    /**
     * Récupère le nom du paramètre (depuis @Param ou le nom réel).
     */
    private static String getParamName(Parameter param) {
        Param p = param.getAnnotation(Param.class);
        if (p != null) {
            return p.value();
        }
        return param.getName();
    }

    /**
     * Bind un objet complexe depuis les paramètres de la requête.
     */
    private static Object bindObjectParam(HttpServletRequest req, Matcher matched, 
                                          MethodMapping mapping, Class<?> paramType) throws Exception {
        Object instance = paramType.getDeclaredConstructor().newInstance();

        // Variables de l'URL
        for (int j = 0; j < mapping.getVariables().size(); j++) {
            String varName = mapping.getVariables().get(j);
            String varValue = matched.group(j + 1);
            trySetField(instance, varName, varValue);
        }

        // Paramètres GET/POST
        Map<String, String[]> requestParams = req.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            String val = (values != null && values.length > 0) ? values[0] : null;
            trySetField(instance, key, val);
        }

        return instance;
    }

    /**
     * Tente de définir un champ d'un objet.
     */
    private static void trySetField(Object instance, String fieldName, String stringValue) {
        if (stringValue == null) return;
        Class<?> cls = instance.getClass();
        try {
            Field f = cls.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object converted = TypeConverter.convert(stringValue, f.getType());
            f.set(instance, converted);
        } catch (NoSuchFieldException e) {
            // champ introuvable → on ignore
        } catch (Exception e) {
            // problème de conversion/affectation → on ignore
        }
    }
}
