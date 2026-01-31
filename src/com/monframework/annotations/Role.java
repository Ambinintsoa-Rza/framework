package com.monframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour restreindre l'accès à une méthode selon le rôle de l'utilisateur.
 * Le rôle est vérifié dans la session (clé "role" ou "profile" par défaut).
 * 
 * Exemple d'utilisation:
 * <pre>
 * @Role("admin")
 * @GetMapping("/admin/dashboard")
 * public ModelView adminDashboard() {
 *     return new ModelView("admin.jsp");
 * }
 * 
 * @Role({"admin", "manager"})  // Plusieurs rôles autorisés
 * @GetMapping("/reports")
 * public ModelView reports() {
 *     return new ModelView("reports.jsp");
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Role {
    /**
     * Le ou les rôles autorisés à accéder à cette méthode.
     */
    String[] value();
    
    /**
     * La clé dans la session où se trouve le rôle de l'utilisateur.
     * Par défaut "profile".
     */
    String sessionKey() default "profile";
}
