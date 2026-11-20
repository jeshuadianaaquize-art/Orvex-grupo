package com.orvex.common.web;

/** Otro microservicio no respondió a tiempo o devolvió un error. */
public class ServicioNoDisponibleException extends RuntimeException {

    private final String servicio;

    public ServicioNoDisponibleException(String servicio, Throwable causa) {
        super("El servicio de " + servicio + " no está disponible en este momento. Intenta de nuevo en unos segundos.", causa);
        this.servicio = servicio;
    }

    public String getServicio() {
        return servicio;
    }
}
