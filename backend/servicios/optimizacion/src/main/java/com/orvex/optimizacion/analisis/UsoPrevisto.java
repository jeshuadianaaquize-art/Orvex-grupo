package com.orvex.optimizacion.analisis;

/** Uso que el cliente le dará a la PC (configuracion_pc.uso_previsto). */
public enum UsoPrevisto {
    JUEGOS("juegos"), EDICION("edición"), OFICINA("oficina");

    private final String texto;

    UsoPrevisto(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }
}
