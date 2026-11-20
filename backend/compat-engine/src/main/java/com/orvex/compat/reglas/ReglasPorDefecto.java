package com.orvex.compat.reglas;

import static com.orvex.compat.Categoria.*;

import com.orvex.compat.Categoria;
import com.orvex.compat.Regla;
import java.util.List;

/** Reglas iniciales, en el mismo formato que la tabla regla_compatibilidad (RNF-06). */
public final class ReglasPorDefecto {

    public record Definicion(String nombre, Categoria a, Categoria b, String condicion) {
    }

    public static final List<Definicion> DEFINICIONES = List.of(
            new Definicion("Socket del procesador", PROCESADOR, PLACA_MADRE, "a.socket = b.socket"),
            new Definicion("Tipo de memoria RAM", RAM, PLACA_MADRE, "a.tipo_ram = b.tipo_ram"),
            new Definicion("Frecuencia de la memoria", RAM, PLACA_MADRE, "a.frecuencia_mhz <= b.frecuencia_max_mhz [advertencia]"),
            new Definicion("Formato de placa y gabinete", GABINETE, PLACA_MADRE, "a.factores_forma contiene b.factor_forma"),
            new Definicion("Largo de la tarjeta gráfica", TARJETA_GRAFICA, GABINETE, "a.largo_mm <= b.max_largo_gpu_mm"),
            new Definicion("Altura del cooler", REFRIGERACION, GABINETE, "a.altura_mm <= b.max_altura_cooler_mm"),
            new Definicion("Cooler y socket", REFRIGERACION, PROCESADOR, "a.sockets contiene b.socket"),
            new Definicion("Interfaz de almacenamiento", ALMACENAMIENTO, PLACA_MADRE, "a.interfaz en b.interfaces_almacenamiento"),
            new Definicion("Conectores de la fuente", TARJETA_GRAFICA, FUENTE, "a.conectores_pcie <= b.conectores_pcie"),
            new Definicion("Consumo y fuente de poder", PROCESADOR, FUENTE, "consumo(base=100) * 1.30 <= b.potencia_w"));

    private ReglasPorDefecto() {
    }

    /** Convierte una fila de regla_compatibilidad en una regla del motor. */
    public static Regla crear(Long id, String nombre, Categoria a, Categoria b, String condicion) {
        return ReglaConsumo.esConsumo(condicion)
                ? ReglaConsumo.parsear(id, nombre, condicion)
                : ReglaCondicion.parsear(id, nombre, a, b, condicion);
    }

    public static List<Regla> crear() {
        return DEFINICIONES.stream().map(d -> crear(null, d.nombre(), d.a(), d.b(), d.condicion())).toList();
    }
}
