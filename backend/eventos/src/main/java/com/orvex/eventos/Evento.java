package com.orvex.eventos;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Hecho de negocio que ya ocurrió (pago confirmado, cambio de precio, ajuste de stock…). */
public record Evento(String id, String tipo, String servicio, Long usuarioId, String entidad, String entidadId,
                     Map<String, Object> datos, Instant fecha) {

    public static Evento nuevo(String tipo, String servicio, Long usuarioId, String entidad, Object entidadId, Map<String, Object> datos) {
        return new Evento(UUID.randomUUID().toString(), tipo, servicio, usuarioId, entidad,
                entidadId == null ? null : String.valueOf(entidadId), datos == null ? Map.of() : datos, Instant.now());
    }

    /** Lectura cómoda de un dato numérico (Jackson puede entregar Integer, Long o Double). */
    public Long numero(String clave) {
        Object v = datos == null ? null : datos.get(clave);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String texto(String clave) {
        Object v = datos == null ? null : datos.get(clave);
        return v == null ? null : String.valueOf(v);
    }
}
