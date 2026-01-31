package com.monframework.handler;

import com.monframework.annotations.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Parameter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestion de la session utilisateur.
 */
public class SessionHandler {

    /**
     * Charge la session HTTP dans une Map.
     */
    public static Map<String, Object> loadSession(HttpServletRequest req) {
        HttpSession httpSession = req.getSession(true);
        Map<String, Object> sessionMap = new HashMap<>();

        Enumeration<String> attrNames = httpSession.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String name = attrNames.nextElement();
            sessionMap.put(name, httpSession.getAttribute(name));
        }

        return sessionMap;
    }

    /**
     * Synchronise une Map vers la session HTTP.
     */
    public static void syncSession(HttpServletRequest req, Map<String, Object> sessionMap) {
        HttpSession httpSession = req.getSession(true);

        for (Map.Entry<String, Object> entry : sessionMap.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Synchronise tous les paramètres @Session après l'exécution d'une méthode.
     */
    @SuppressWarnings("unchecked")
    public static void syncAllSessions(HttpServletRequest req, Parameter[] params, Object[] args) {
        for (int i = 0; i < params.length; i++) {
            if (params[i].isAnnotationPresent(Session.class) && args[i] instanceof Map) {
                Map<String, Object> sessionMap = (Map<String, Object>) args[i];
                syncSession(req, sessionMap);
            }
        }
    }

    /**
     * Invalide la session courante.
     */
    public static void invalidateSession(HttpServletRequest req) {
        HttpSession httpSession = req.getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }
    }

    /**
     * Récupère une valeur de la session.
     */
    public static Object getSessionAttribute(HttpServletRequest req, String key) {
        HttpSession httpSession = req.getSession(false);
        if (httpSession != null) {
            return httpSession.getAttribute(key);
        }
        return null;
    }
}
