package com.orvex.optimizacion;

import com.orvex.compat.Categoria;
import com.orvex.compat.reglas.ReglasPorDefecto;
import com.orvex.optimizacion.clientes.CatalogoCliente.Producto;
import com.orvex.optimizacion.clientes.CompatibilidadCliente.Regla;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Catálogo de prueba con las mismas especificaciones que los datos de demostración. */
final class Datos {

    private Datos() {
    }

    static Producto p(long id, Categoria c, String precio, int stock, Map<String, String> specs) {
        return new Producto(id, "SKU-" + id, "Producto " + id, "Marca", new BigDecimal(precio), null, c, specs, stock, stock > 0);
    }

    static final Producto CPU_5600 = p(1, Categoria.PROCESADOR, "1100", 12,
            Map.of("socket", "AM4", "tdp_w", "65", "nucleos", "6", "rendimiento", "58"));
    static final Producto CPU_5700X = p(18, Categoria.PROCESADOR, "1500", 6,
            Map.of("socket", "AM4", "tdp_w", "65", "nucleos", "8", "rendimiento", "66"));
    static final Producto CPU_7600 = p(2, Categoria.PROCESADOR, "1450", 8,
            Map.of("socket", "AM5", "tdp_w", "65", "nucleos", "6", "rendimiento", "72"));
    static final Producto PLACA_AM4 = p(4, Categoria.PLACA_MADRE, "780", 9, Map.of("socket", "AM4", "tipo_ram", "DDR4",
            "factor_forma", "mATX", "interfaces_almacenamiento", "M.2,SATA", "frecuencia_max_mhz", "4400"));
    static final Producto RAM_16_UNO = p(7, Categoria.RAM, "380", 25,
            Map.of("tipo_ram", "DDR4", "capacidad_gb", "16", "frecuencia_mhz", "3200", "modulos", "1"));
    static final Producto RAM_32_DOS = p(23, Categoria.RAM, "760", 14,
            Map.of("tipo_ram", "DDR4", "capacidad_gb", "32", "frecuencia_mhz", "3600", "modulos", "2"));
    static final Producto RAM_32_SIN_STOCK = p(31, Categoria.RAM, "700", 0,
            Map.of("tipo_ram", "DDR4", "capacidad_gb", "32", "frecuencia_mhz", "3600", "modulos", "2"));
    static final Producto GPU_4060 = p(9, Categoria.TARJETA_GRAFICA, "3300", 5,
            Map.of("tdp_w", "115", "largo_mm", "240", "conectores_pcie", "1", "rendimiento", "58"));
    static final Producto GPU_7800 = p(26, Categoria.TARJETA_GRAFICA, "4300", 4,
            Map.of("tdp_w", "263", "largo_mm", "267", "conectores_pcie", "2", "rendimiento", "88"));
    static final Producto SSD = p(11, Categoria.ALMACENAMIENTO, "520", 20, Map.of("interfaz", "M.2", "capacidad_gb", "1000"));
    static final Producto HDD = p(28, Categoria.ALMACENAMIENTO, "380", 16, Map.of("interfaz", "SATA", "capacidad_gb", "2000"));
    static final Producto FTE_450 = p(16, Categoria.FUENTE, "330", 8, Map.of("potencia_w", "450", "conectores_pcie", "1"));
    static final Producto FTE_650 = p(17, Categoria.FUENTE, "430", 11, Map.of("potencia_w", "650", "conectores_pcie", "2"));
    static final Producto FTE_750 = p(30, Categoria.FUENTE, "550", 7, Map.of("potencia_w", "750", "conectores_pcie", "2"));
    static final Producto GABINETE = p(12, Categoria.GABINETE, "450", 6,
            Map.of("factores_forma", "ATX,mATX,ITX", "max_largo_gpu_mm", "330", "max_altura_cooler_mm", "160"));

    static final List<Producto> CATALOGO = List.of(CPU_5600, CPU_5700X, CPU_7600, PLACA_AM4, RAM_16_UNO, RAM_32_DOS,
            RAM_32_SIN_STOCK, GPU_4060, GPU_7800, SSD, HDD, FTE_450, FTE_650, FTE_750, GABINETE);

    /** Las 10 reglas iniciales, con ids 1..10 como en la tabla regla_compatibilidad. */
    static List<Regla> reglas() {
        List<Regla> r = new ArrayList<>();
        long id = 1;
        for (var d : ReglasPorDefecto.DEFINICIONES) {
            r.add(new Regla(id++, d.a(), d.b(), d.nombre(), d.condicion()));
        }
        return r;
    }
}
