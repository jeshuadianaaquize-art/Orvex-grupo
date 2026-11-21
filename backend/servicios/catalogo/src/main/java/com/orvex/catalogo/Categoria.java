package com.orvex.catalogo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Las 8 categorías del informe */
public enum Categoria {
    PROCESADOR("Procesador", List.of(
            Spec.texto("socket", "Socket", true),
            Spec.numero("tdp_w", "Consumo (TDP)", "W", true),
            Spec.numero("nucleos", "Núcleos", null, false),
            Spec.numero("hilos", "Hilos", null, false),
            Spec.numero("frecuencia_ghz", "Frecuencia máxima", "GHz", false),
            Spec.numero("rendimiento", "Índice de rendimiento (1-100)", null, false))),
    PLACA_MADRE("Placa madre", List.of(
            Spec.texto("socket", "Socket", true),
            Spec.texto("tipo_ram", "Tipo de memoria", true),
            Spec.texto("factor_forma", "Formato", true),
            Spec.lista("interfaces_almacenamiento", "Interfaces de almacenamiento", true),
            Spec.numero("frecuencia_max_mhz", "Frecuencia máxima de memoria", "MHz", false),
            Spec.numero("ranuras_ram", "Ranuras de RAM", null, false),
            Spec.numero("ranuras_m2", "Ranuras M.2", null, false))),
    RAM("Memoria RAM", List.of(
            Spec.texto("tipo_ram", "Tipo de memoria", true),
            Spec.numero("capacidad_gb", "Capacidad", "GB", false),
            Spec.numero("frecuencia_mhz", "Frecuencia", "MHz", false),
            Spec.numero("modulos", "Módulos", null, false))),
    TARJETA_GRAFICA("Tarjeta gráfica", List.of(
            Spec.numero("largo_mm", "Largo", "mm", true),
            Spec.numero("tdp_w", "Consumo (TDP)", "W", true),
            Spec.numero("conectores_pcie", "Conectores PCIe de 8 pines", null, true),
            Spec.numero("vram_gb", "Memoria de video", "GB", false),
            Spec.numero("rendimiento", "Índice de rendimiento (1-100)", null, false))),
    ALMACENAMIENTO("Almacenamiento", List.of(
            Spec.texto("interfaz", "Interfaz", true),
            Spec.numero("capacidad_gb", "Capacidad", "GB", false))),
    FUENTE("Fuente de poder", List.of(
            Spec.numero("potencia_w", "Potencia", "W", true),
            Spec.numero("conectores_pcie", "Conectores PCIe de 8 pines", null, true),
            Spec.texto("certificacion", "Certificación", false))),
    GABINETE("Gabinete", List.of(
            Spec.lista("factores_forma", "Formatos de placa admitidos", true),
            Spec.numero("max_largo_gpu_mm", "Largo máximo de gráfica", "mm", true),
            Spec.numero("max_altura_cooler_mm", "Altura máxima de cooler", "mm", true))),
    REFRIGERACION("Refrigeración", List.of(
            Spec.numero("altura_mm", "Altura", "mm", true),
            Spec.lista("sockets", "Sockets compatibles", true)));

    public enum Tipo { TEXTO, NUMERO, LISTA }

    /** Una especificación conocida: clave, etiqueta, tipo de valor, unidad y si es requerida para publicar. */
    public record Spec(String clave, String etiqueta, Tipo tipo, String unidad, boolean requerida) {
        static Spec texto(String clave, String etiqueta, boolean requerida) {
            return new Spec(clave, etiqueta, Tipo.TEXTO, null, requerida);
        }

        static Spec numero(String clave, String etiqueta, String unidad, boolean requerida) {
            return new Spec(clave, etiqueta, Tipo.NUMERO, unidad, requerida);
        }

        static Spec lista(String clave, String etiqueta, boolean requerida) {
            return new Spec(clave, etiqueta, Tipo.LISTA, null, requerida);
        }
    }

    private final String nombreBd;
    private final List<Spec> especificaciones;

    Categoria(String nombreBd, List<Spec> especificaciones) {
        this.nombreBd = nombreBd;
        this.especificaciones = especificaciones;
    }

    public String nombreBd() {
        return nombreBd;
    }

    public List<Spec> especificaciones() {
        return especificaciones;
    }

    public Optional<Spec> spec(String clave) {
        return especificaciones.stream().filter(s -> s.clave().equals(clave)).findFirst();
    }

    public static Categoria desdeBd(String nombre) {
        return Arrays.stream(values()).filter(c -> c.nombreBd.equals(nombre)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Categoría desconocida en la base de datos: " + nombre));
    }
}
