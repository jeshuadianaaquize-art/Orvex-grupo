package com.orvex.compat;

/** Resultado de una regla (RF-07) */
public record ResultadoRegla(Long reglaId, String regla, Severidad severidad, String mensaje) {
}
