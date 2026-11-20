package com.orvex.usuarios.modelo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

/** Dominio de usuario.estado: 'activo' o 'bloqueado' (diccionario de datos). */
public enum EstadoUsuario {
    ACTIVO, BLOQUEADO;

    /** Guarda el valor en minúsculas, como lo define el ENUM de la base de datos. */
    @Converter(autoApply = true)
    public static class ConvertidorBd implements AttributeConverter<EstadoUsuario, String> {
        @Override
        public String convertToDatabaseColumn(EstadoUsuario e) {
            return e == null ? null : e.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public EstadoUsuario convertToEntityAttribute(String v) {
            return v == null ? null : EstadoUsuario.valueOf(v.toUpperCase(Locale.ROOT));
        }
    }
}
