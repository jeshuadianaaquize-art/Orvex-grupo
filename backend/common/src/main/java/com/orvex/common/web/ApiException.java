package com.orvex.common.web;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Error de negocio con su código HTTP y, opcionalmente, el mensaje de cada campo */
public class ApiException extends RuntimeException {

    private final HttpStatus estado;
    private final Map<String, String> campos;

    public ApiException(HttpStatus estado, String mensaje) {
        this(estado, mensaje, Map.of());
    }

    public ApiException(HttpStatus estado, String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.estado = estado;
        this.campos = Map.copyOf(campos);
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public Map<String, String> getCampos() {
        return campos;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje);
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje);
    }

    public static ApiException noAutenticado(String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensaje);
    }

    public static ApiException conflicto(String campo, String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje, Map.of(campo, mensaje));
    }
}
