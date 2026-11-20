package com.orvex.compatibilidad.modelo;

import com.orvex.compat.Severidad;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/** Dominios de valores del esquema orvex_compatibilidad (en la base de datos van en minúsculas). */
public final class Modelo {

    private Modelo() {
    }

    /** configuracion_pc.uso_previsto (lo usa también el análisis de optimización). */
    public enum UsoPrevisto { JUEGOS, EDICION, OFICINA }

    @Converter(autoApply = true)
    public static class UsoConvertidor implements AttributeConverter<UsoPrevisto, String> {
        @Override
        public String convertToDatabaseColumn(UsoPrevisto u) {
            return u == null ? null : u.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public UsoPrevisto convertToEntityAttribute(String v) {
            return v == null ? null : UsoPrevisto.valueOf(v.toUpperCase(Locale.ROOT));
        }
    }

    @Converter(autoApply = true)
    public static class SeveridadConvertidor implements AttributeConverter<Severidad, String> {
        @Override
        public String convertToDatabaseColumn(Severidad s) {
            return s == null ? null : s.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public Severidad convertToEntityAttribute(String v) {
            return v == null ? null : Severidad.valueOf(v.toUpperCase(Locale.ROOT));
        }
    }
}
