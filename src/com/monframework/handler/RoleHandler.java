package com.monframework.handler;

import com.monframework.annotations.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

/**
 * Gestion des rôles et autorisations.
 */
public class RoleHandler {

    /**
     * Vérifie si l'utilisateur a le rôle requis pour accéder à une méthode.
     * @return true si autorisé, false sinon
     */
    public static boolean checkRole(HttpServletRequest req, Method method) {
        if (!method.isAnnotationPresent(Role.class)) {
            return true; // Pas de restriction de rôle
        }

        Role roleAnnotation = method.getAnnotation(Role.class);
        String[] allowedRoles = roleAnnotation.value();
        String sessionKey = roleAnnotation.sessionKey();

        HttpSession httpSession = req.getSession(false);
        String userRole = null;

        if (httpSession != null) {
            Object roleObj = httpSession.getAttribute(sessionKey);
            if (roleObj != null) {
                userRole = roleObj.toString();
            }
        }

        // Vérifier si l'utilisateur a un des rôles autorisés
        if (userRole != null) {
            for (String allowedRole : allowedRoles) {
                if (allowedRole.equals(userRole)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Envoie une réponse 403 Forbidden.
     */
    public static void sendForbiddenResponse(HttpServletResponse resp, Method method) throws IOException {
        Role roleAnnotation = method.getAnnotation(Role.class);
        String[] allowedRoles = roleAnnotation.value();

        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.setContentType("text/html;charset=UTF-8");

        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html><html><head><title>Accès refusé</title></head>");
        out.println("<body><h1>403 - Accès refusé</h1>");
        out.println("<p>Vous n'avez pas les droits nécessaires pour accéder à cette ressource.</p>");
        out.println("<p>Rôle requis : " + String.join(" ou ", allowedRoles) + "</p>");
        out.println("<p><a href=\"login\">Se connecter</a></p>");
        out.println("</body></html>");
    }
}
