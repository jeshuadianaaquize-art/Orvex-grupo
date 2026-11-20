package com.orvex.compat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** El catálogo guarda cada especificación como texto */
public final class Especificaciones {

    private Especificaciones() {
    }

    public static Map<String, Object> tipar(Map<String, String> specs) {
        Map<String, Object> r = new LinkedHashMap<>();
        if (specs != null) {
            specs.forEach((k, v) -> {
                if (v != null) {
                    r.put(k, valor(v.trim()));
                }
            });
        }
        return r;
    }

    static Object valor(String v) {
        if (v.contains(",")) {
            return Arrays.stream(v.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        if (v.matches("^-?\\d+(\\.\\d+)?$")) {
            BigDecimal n = new BigDecimal(v);
            return n.scale() <= 0 ? (Object) n.longValueExact() : (Object) n.doubleValue();
        }
        return v;
    }
}
