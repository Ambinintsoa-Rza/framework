package com.monframework;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;

import com.monframework.annotations.Controller;
import com.monframework.annotations.UrlMapping;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String context = req.getContextPath();
        String relativePath = uri.substring(context.length());

        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        try {
            // Charger dynamiquement ton contrôleur de test
            Class<?> cls = Class.forName("com.test.MyController");

            if (cls.isAnnotationPresent(Controller.class)) {
                Object controllerInstance = cls.getDeclaredConstructor().newInstance();

                for (Method m : cls.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(UrlMapping.class)) {
                        UrlMapping mapping = m.getAnnotation(UrlMapping.class);

                        if (mapping.value().equals(relativePath)) {
                            Object result = m.invoke(controllerInstance);
                            out.println(result);
                            return;
                        }
                    }
                }
            }

            out.println("Aucune méthode trouvée pour : " + relativePath);

        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
}
