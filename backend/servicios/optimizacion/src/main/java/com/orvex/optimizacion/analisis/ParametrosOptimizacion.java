package com.orvex.optimizacion.analisis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Umbrales del optimizador */
@ConfigurationProperties("orvex.optimizacion")
public record ParametrosOptimizacion(
        /** Bajo este puntaje se buscan alternativas aunque todo sea compatible (RF-10: "baja optimización"). */
        int umbralAlternativas,
        int alternativasPorCategoria,
        /** Tope del puntaje mientras haya incompatibilidades: esa PC no se puede comprar tal como está. */
        int puntajeMaximoIncompatible,
        Penalizaciones penalizaciones,
        Usos usos) {

    /** Puntos que se restan de 100 por cada hallazgo. */
    public record Penalizaciones(
            int incompatible,
            int advertencia,
            int maximoCompatibilidad,
            int balance,
            int nucleos,
            int ram,
            int dobleCanal,
            int sinSsd,
            int gasto) {
    }

    /** Lo que pide cada uso */
    public record Uso(
            double balanceMinimo,
            double balanceMaximo,
            int ramMinimaGb,
            int nucleosMinimos,
            /** Puntos que se restan si no hay tarjeta gráfica (0 = no hace falta). */
            int sinGrafica,
            /** Rendimiento de gráfica a partir del cual es gasto innecesario para este uso (0 = sin tope). */
            int graficaMaximaUtil) {
    }

    public record Usos(Uso juegos, Uso edicion, Uso oficina) {
        public Uso de(UsoPrevisto u) {
            return switch (u) {
                case JUEGOS -> juegos;
                case EDICION -> edicion;
                case OFICINA -> oficina;
            };
        }
    }

    /** Los mismos valores de application.yml; una prueba comprueba que no se desincronicen. */
    public static ParametrosOptimizacion propuestos() {
        return new ParametrosOptimizacion(70, 3, 40,
                new Penalizaciones(25, 5, 50, 30, 10, 20, 8, 8, 10),
                new Usos(
                        new Uso(0.9, 1.6, 16, 6, 30, 0),
                        new Uso(0.7, 1.3, 32, 8, 15, 0),
                        new Uso(0, 0, 8, 4, 0, 40)));
    }
}
