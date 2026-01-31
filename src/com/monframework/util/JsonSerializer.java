package com.monframework.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

/**
 * Utilitaire de sérialisation JSON.
 */
public class JsonSerializer {

    /**
     * Échappe les caractères spéciaux pour JSON.
     */
    public static String escape(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * Convertit un objet en JSON.
     */
    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return '"' + escape((String)obj) + '"';
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);

        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Object eObj : ((Map<?,?>)obj).entrySet()) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) eObj;
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(String.valueOf(e.getKey()))).append('"').append(':')
                  .append(toJson(e.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }

        if (obj instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (Object item : (Collection<?>)obj) {
                if (!first) sb.append(',');
                first = false;
                sb.append(toJson(item));
            }
            sb.append(']');
            return sb.toString();
        }

        if (obj.getClass().isArray()) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            int len = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                Object v = java.lang.reflect.Array.get(obj, i);
                sb.append(toJson(v));
            }
            sb.append(']');
            return sb.toString();
        }

        // Fallback POJO: sérialiser ses champs déclarés
        return serializePojo(obj);
    }

    /**
     * Sérialise un POJO en JSON.
     */
    private static String serializePojo(Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        Class<?> c = obj.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod)) continue;
                f.setAccessible(true);
                try {
                    Object v = f.get(obj);
                    if (!first) sb.append(',');
                    first = false;
                    sb.append('"').append(escape(f.getName())).append('"').append(':')
                      .append(toJson(v));
                } catch (IllegalAccessException ignored) {}
            }
            c = c.getSuperclass();
        }
        sb.append('}');
        return sb.toString();
    }
}
