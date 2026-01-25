package com.monframework.model;

import java.io.*;

/**
 * Sprint 10 : Représente un fichier uploadé
 */
public class UploadedFile {
    private String fileName;       // Nom original du fichier
    private String contentType;    // Type MIME (image/png, application/pdf, etc.)
    private byte[] content;        // Contenu binaire du fichier
    private long size;             // Taille en octets

    public UploadedFile() {}

    public UploadedFile(String fileName, String contentType, byte[] content) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
        this.size = (content != null) ? content.length : 0;
    }

    // === Getters ===
    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public long getSize() {
        return size;
    }

    // === Setters ===
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setContent(byte[] content) {
        this.content = content;
        this.size = (content != null) ? content.length : 0;
    }

    // === Méthodes utilitaires ===

    /**
     * Sauvegarde le fichier sur le disque
     * @param destPath Chemin complet de destination (ex: "C:/uploads/monimage.png")
     */
    public void saveTo(String destPath) throws IOException {
        File dest = new File(destPath);
        dest.getParentFile().mkdirs(); // créer les dossiers si nécessaire
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(content);
        }
    }

    /**
     * Sauvegarde le fichier dans un dossier avec son nom original
     * @param directory Dossier de destination
     */
    public void saveToDirectory(String directory) throws IOException {
        saveTo(directory + File.separator + fileName);
    }

    /**
     * Retourne l'extension du fichier (ex: "png", "pdf")
     */
    public String getExtension() {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Vérifie si c'est une image
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    @Override
    public String toString() {
        return "UploadedFile{" +
                "fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                '}';
    }
}
