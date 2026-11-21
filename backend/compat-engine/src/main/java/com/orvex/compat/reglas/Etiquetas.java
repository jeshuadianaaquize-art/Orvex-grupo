package com.orvex.compat.reglas;

import java.util.Map;

/** Nombre legible y unidad de cada especificación, para que los mensajes de las reglas se entiendan. */
public final class Etiquetas {

    public record Etiqueta(String nombre, String unidad) {
    }

    private static final Map<String, Etiqueta> CONOCIDAS = Map.ofEntries(
            Map.entry("socket", new Etiqueta("socket", null)),
            Map.entry("sockets", new Etiqueta("sockets admitidos", null)),
            Map.entry("tdp_w", new Etiqueta("consumo", "W")),
            Map.entry("tipo_ram", new Etiqueta("tipo de memoria", null)),
            Map.entry("frecuencia_mhz", new Etiqueta("frecuencia", "MHz")),
            Map.entry("frecuencia_max_mhz", new Etiqueta("frecuencia máxima de memoria", "MHz")),
            Map.entry("factor_forma", new Etiqueta("formato", null)),
            Map.entry("factores_forma", new Etiqueta("formatos de placa", null)),
            Map.entry("largo_mm", new Etiqueta("largo", "mm")),
            Map.entry("max_largo_gpu_mm", new Etiqueta("largo máximo de gráfica", "mm")),
            Map.entry("altura_mm", new Etiqueta("altura", "mm")),
            Map.entry("max_altura_cooler_mm", new Etiqueta("altura máxima de cooler", "mm")),
            Map.entry("interfaz", new Etiqueta("interfaz", null)),
            Map.entry("interfaces_almacenamiento", new Etiqueta("interfaces de almacenamiento", null)),
            Map.entry("conectores_pcie", new Etiqueta("conectores PCIe de 8 pines", null)),
            Map.entry("potencia_w", new Etiqueta("potencia", "W")));

    private Etiquetas() {
    }

    public static Etiqueta de(String clave) {
        return CONOCIDAS.getOrDefault(clave, new Etiqueta(clave.replace('_', ' '), null));
    }
}
