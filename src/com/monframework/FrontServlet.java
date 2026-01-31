package com.monframework;

import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;

import com.monframework.annotations.Json;
import com.monframework.handler.ArgumentResolver;
import com.monframework.handler.ResponseHandler;
import com.monframework.handler.RoleHandler;
import com.monframework.handler.SessionHandler;
import com.monframework.mapping.MethodMapping;
import com.monframework.util.ControllerScanner;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,      // 1 MB avant écriture sur disque
    maxFileSize = 1024 * 1024 * 10,       // 10 MB max par fichier
    maxRequestSize = 1024 * 1024 * 50     // 50 MB max pour la requête totale
)
public class FrontServlet extends HttpServlet {

    private Map<String, MethodMapping> mappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        try {
            ControllerScanner scanner = new ControllerScanner(getServletContext());
            mappings = scanner.scanPackage("com.test");
            ControllerScanner.printMappings(mappings);
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
        String httpMethod = req.getMethod();

        try {
            // 1. Trouver le mapping correspondant
            MethodMapping mapping = findMapping(relativePath, httpMethod);

            if (mapping == null) {
                ResponseHandler.sendNotFound(resp, relativePath);
                return;
            }

            // 2. Vérifier les rôles
            if (!RoleHandler.checkRole(req, mapping.getMethod())) {
                RoleHandler.sendForbiddenResponse(resp, mapping.getMethod());
                return;
            }

            // 3. Debug info (si pas JSON)
            if (!mapping.getMethod().isAnnotationPresent(Json.class)) {
                printDebugInfo(resp, mapping);
            }

            // 4. Créer l'instance du contrôleur
            Object controllerInstance = mapping.getControllerClass()
                    .getDeclaredConstructor().newInstance();

            // 5. Résoudre les arguments
            Matcher matched = mapping.match(relativePath);
            matched.matches(); // Nécessaire pour activer les groupes
            Object[] args = ArgumentResolver.resolveArguments(req, mapping, matched);

            // 6. Appeler la méthode
            Object result = mapping.getMethod().invoke(controllerInstance, args);

            // 7. Synchroniser les sessions
            Parameter[] params = mapping.getMethod().getParameters();
            SessionHandler.syncAllSessions(req, params, args);

            // 8. Gérer la réponse
            ResponseHandler.handleResponse(req, resp, mapping.getMethod(), result);

        } catch (Exception e) {
            ResponseHandler.sendError(resp, e);
        }
    }

    /**
     * Trouve le mapping correspondant à l'URL et la méthode HTTP.
     */
    private MethodMapping findMapping(String path, String httpMethod) {
        for (MethodMapping mm : mappings.values()) {
            Matcher mat = mm.match(path);
            if (mat.matches() && mm.isHttpMethodCompatible(httpMethod)) {
                return mm;
            }
        }
        return null;
    }

    /**
     * Affiche les infos de debug.
     */
    private void printDebugInfo(HttpServletResponse resp, MethodMapping mapping) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("=== ROUTE TROUVÉE ===");
        out.println("Controller : " + mapping.getControllerClass().getName());
        out.println("Méthode    : " + mapping.getMethod().getName());
        out.println("=======================");
    }
}
