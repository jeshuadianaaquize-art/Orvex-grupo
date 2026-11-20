package com.orvex.seguridad;

import java.util.Locale;

/** Roles del sistema (RF-21). El nombre en minúsculas es el valor guardado en la tabla "rol". */
public enum Rol {
    CLIENTE, VENDEDOR, ALMACENERO, ADMINISTRADOR;

    public String nombreBd() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Rol desde(String valor) {
        return valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }
}
