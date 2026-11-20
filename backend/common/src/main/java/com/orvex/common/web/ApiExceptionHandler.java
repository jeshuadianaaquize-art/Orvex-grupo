package com.orvex.common.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Respuesta de error común a todos los servicios */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> negocio(ApiException e) {
        return error(e.getEstado(), e.getMessage(), e.getCampos());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> solicitudInvalida(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String mensaje = campos.size() == 1
                ? campos.keySet().iterator().next() + ": " + campos.values().iterator().next()
                : "Revisa los datos marcados.";
        return error(HttpStatus.BAD_REQUEST, mensaje, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido.", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro '" + e.getName() + "'.", Map.of());
    }

    /** Las rutas con sesión reciben la identidad del gateway; si falta, la petición no pasó por él. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<Map<String, Object>> sinIdentidad(MissingRequestHeaderException e) {
        return error(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.", Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> archivoGrande(MaxUploadSizeExceededException e) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo es demasiado grande (máximo 2 MB).", Map.of());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    ResponseEntity<Map<String, Object>> servicioCaido(ServicioNoDisponibleException e) {
        log.warn("Dependencia no disponible: {}", e.getServicio(), e.getCause());
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), Map.of());
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus estado, String mensaje, Map<String, String> campos) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", mensaje == null ? "Solicitud inválida." : mensaje);
        if (!campos.isEmpty()) {
            cuerpo.put("campos", campos);
        }
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
