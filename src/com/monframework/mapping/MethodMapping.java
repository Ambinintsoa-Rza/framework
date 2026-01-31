package com.monframework.mapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe représentant un mapping URL vers une méthode de contrôleur.
 */
public class MethodMapping {
    
    private final Class<?> controllerClass;
    private final Method method;
    private final String httpMethod; // GET, POST ou ANY
    private final Pattern regex;
    private final List<String> variables;

    public MethodMapping(Class<?> controllerClass, Method method, String httpMethod, 
                         Pattern regex, List<String> variables) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.httpMethod = httpMethod;
        this.regex = regex;
        this.variables = variables;
    }

    /**
     * Factory method pour créer un MethodMapping à partir d'une URL.
     */
    public static MethodMapping build(Class<?> cls, Method m, String url, String httpMethod) {
        List<String> variables = new ArrayList<>();

        Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(url);
        String regexStr = url;

        while (matcher.find()) {
            String varName = matcher.group(1);
            variables.add(varName);
            regexStr = regexStr.replace("{" + varName + "}", "([^/]+)");
        }

        Pattern pattern = Pattern.compile("^" + regexStr + "$");

        return new MethodMapping(cls, m, httpMethod, pattern, variables);
    }

    /**
     * Vérifie si l'URL correspond à ce mapping.
     */
    public Matcher match(String path) {
        return regex.matcher(path);
    }

    /**
     * Vérifie si la méthode HTTP est compatible.
     */
    public boolean isHttpMethodCompatible(String requestMethod) {
        return httpMethod.equals("ANY") || httpMethod.equals(requestMethod);
    }

    // Getters
    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Pattern getRegex() {
        return regex;
    }

    public List<String> getVariables() {
        return variables;
    }
}
