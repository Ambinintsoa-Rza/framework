package com.monframework.handler;

import com.monframework.annotations.Json;
import com.monframework.model.ModelView;
import com.monframework.util.JsonSerializer;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Gestion des réponses (JSON, JSP, etc.).
 */
public class ResponseHandler {

    /**
     * Traite le résultat d'une méthode de contrôleur.
     */
    public static void handleResponse(HttpServletRequest req, HttpServletResponse resp,
                                       Method method, Object result) throws Exception {
        PrintWriter out = resp.getWriter();

        if (method.isAnnotationPresent(Json.class)) {
            handleJsonResponse(resp, result);
        } else if (result instanceof ModelView) {
            handleModelViewResponse(req, resp, (ModelView) result);
        } else {
            out.println("Type de retour non supporté : "
                    + (result != null ? result.getClass() : "null"));
        }
    }

    /**
     * Traite une réponse JSON.
     */
    private static void handleJsonResponse(HttpServletResponse resp, Object result) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String payload;
        if (result instanceof ModelView) {
            payload = JsonSerializer.toJson(((ModelView) result).getData());
        } else {
            payload = JsonSerializer.toJson(result);
        }
        out.print(payload);
    }

    /**
     * Traite une réponse ModelView (forward vers JSP).
     */
    private static void handleModelViewResponse(HttpServletRequest req, HttpServletResponse resp,
                                                 ModelView mv) throws Exception {
        // Mettre les données dans la requête
        for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }

        // Forward vers JSP
        RequestDispatcher dispatcher = req.getRequestDispatcher("/" + mv.getView());
        dispatcher.forward(req, resp);
    }

    /**
     * Envoie une erreur 404.
     */
    public static void sendNotFound(HttpServletResponse resp, String path) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().println("Aucune méthode trouvée pour : " + path);
    }

    /**
     * Envoie une erreur 500 avec le stack trace.
     */
    public static void sendError(HttpServletResponse resp, Exception e) throws IOException {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.setContentType("text/plain;charset=UTF-8");
        e.printStackTrace(resp.getWriter());
    }
}
