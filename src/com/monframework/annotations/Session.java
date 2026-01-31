package com.monframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour injecter la session utilisateur sous forme de Map<String, Object>.
 * À placer sur un paramètre de type Map<String, Object> dans une méthode de contrôleur.
 * 
 * Exemple d'utilisation:
 * <pre>
 * @GetMapping("/dashboard")
 * public ModelView dashboard(@Session Map<String, Object> session) {
 *     String username = (String) session.get("username");
 *     session.put("lastVisit", System.currentTimeMillis());
 *     return new ModelView("dashboard.jsp");
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Session {
}
