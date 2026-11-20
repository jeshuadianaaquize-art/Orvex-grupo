package com.orvex.catalogo.modelo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;
import java.util.Set;

/** Ciclo de vida del producto */
public enum EstadoProducto {
    BORRADOR, PUBLICADO, INACTIVO;

    public boolean puedePasarA(EstadoProducto destino) {
        return switch (this) {
            case BORRADOR -> destino == PUBLICADO;
            case PUBLICADO -> destino == INACTIVO;
            case INACTIVO -> destino == PUBLICADO;
        };
    }

    public static Set<EstadoProducto> visiblesEnTienda() {
        return Set.of(PUBLICADO);
    }

    @Converter(autoApply = true)
    public static class ConvertidorBd implements AttributeConverter<EstadoProducto, String> {
        @Override
        public String convertToDatabaseColumn(EstadoProducto e) {
            return e == null ? null : e.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public EstadoProducto convertToEntityAttribute(String v) {
            return v == null ? null : EstadoProducto.valueOf(v.toUpperCase(Locale.ROOT));
        }
    }
}
