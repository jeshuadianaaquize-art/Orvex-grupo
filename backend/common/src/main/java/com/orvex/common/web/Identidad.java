package com.orvex.common.web;

/** Cabeceras con la identidad del usuario que agrega el gateway después de validar el JWT. */
public final class Identidad {

    public static final String USUARIO_ID = "X-Usuario-Id";
    public static final String USUARIO_ROL = "X-Usuario-Rol";

    private Identidad() {
    }
}
