package com.monframework;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.*;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Récupérer le chemin demandé 
        String uri = req.getRequestURI();
        // Supprimer le contexte
        String context = req.getContextPath();
        String relativePath = uri.substring(context.length());

        // Construire le chemin absolu vers le fichier dans ton projet web
        String realPath = getServletContext().getRealPath(relativePath);

        File file = new File(realPath);

        if (file.exists() && file.isFile()) {
            // Si le fichier existe, lire son contenu et l'afficher
            resp.setContentType(getServletContext().getMimeType(file.getName()));
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream out = resp.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } else {
            // Si le fichier n’existe pas, juste afficher l’URL
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().println("URL tapée : " + uri);
        }
    }
}
