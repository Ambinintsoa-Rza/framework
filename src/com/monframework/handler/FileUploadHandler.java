package com.monframework.handler;

import com.monframework.model.UploadedFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Gestion des uploads de fichiers.
 */
public class FileUploadHandler {

    /**
     * Vérifie si la requête est multipart (upload de fichiers).
     */
    public static boolean isMultipartRequest(HttpServletRequest req) {
        String contentType = req.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    /**
     * Extrait le nom de fichier original depuis le header Content-Disposition.
     */
    public static String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        if (contentDisp != null) {
            for (String token : contentDisp.split(";")) {
                if (token.trim().startsWith("filename")) {
                    String name = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                    // Gérer les chemins Windows (C:\path\file.txt → file.txt)
                    int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
                    if (lastSlash >= 0) {
                        name = name.substring(lastSlash + 1);
                    }
                    return name;
                }
            }
        }
        return null;
    }

    /**
     * Convertit un Part en UploadedFile.
     */
    public static UploadedFile partToUploadedFile(Part part) throws IOException {
        if (part == null) return null;

        String fileName = extractFileName(part);
        if (fileName == null || fileName.isEmpty()) return null;

        String contentType = part.getContentType();

        // Lire le contenu binaire
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = part.getInputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
        }

        return new UploadedFile(fileName, contentType, baos.toByteArray());
    }

    /**
     * Récupère un fichier uploadé par son nom de paramètre.
     */
    public static UploadedFile getUploadedFile(HttpServletRequest req, String paramName) {
        if (!isMultipartRequest(req)) return null;

        try {
            Part part = req.getPart(paramName);
            return partToUploadedFile(part);
        } catch (Exception e) {
            return null;
        }
    }
}
