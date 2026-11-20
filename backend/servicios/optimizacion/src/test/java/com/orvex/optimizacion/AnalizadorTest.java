package com.orvex.optimizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.orvex.compat.Categoria;
import com.orvex.compat.Especificaciones;
import com.orvex.compat.ReporteCompatibilidad;
import com.orvex.compat.ResultadoRegla;
import com.orvex.compat.Severidad;
import com.orvex.optimizacion.analisis.Analizador;
import com.orvex.optimizacion.analisis.Analizador.Hallazgo;
import com.orvex.optimizacion.analisis.Analizador.Pieza;
import com.orvex.optimizacion.analisis.Analizador.Tipo;
import com.orvex.optimizacion.analisis.ParametrosOptimizacion;
import com.orvex.optimizacion.analisis.UsoPrevisto;
import com.orvex.optimizacion.clientes.CatalogoCliente.Producto;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnalizadorTest {

    private final Analizador analizador = new Analizador(ParametrosOptimizacion.propuestos());
    private static final ReporteCompatibilidad SIN_PROBLEMAS = new ReporteCompatibilidad(Severidad.COMPATIBLE, List.of(), 0);

    private static List<Pieza> piezas(Producto... ps) {
        return Arrays.stream(ps).map(p -> new Pieza(p.id(), p.categoria(), p.nombre(), p.precio(),
                Especificaciones.tipar(p.specs()))).toList();
    }

    private static Hallazgo de(Analizador.Resultado r, Tipo t) {
        return r.recomendaciones().stream().filter(h -> h.tipo() == t).findFirst().orElseThrow();
    }

    @Test
    void unArmadoEquilibradoParaJuegosSaca100() {
        var r = analizador.analizar(piezas(Datos.CPU_5700X, Datos.PLACA_AM4, Datos.RAM_32_DOS, Datos.GPU_7800, Datos.SSD,
                Datos.FTE_750, Datos.GABINETE), UsoPrevisto.JUEGOS, SIN_PROBLEMAS);
        assertEquals(100, r.puntaje(), r.recomendaciones().toString());
        assertNull(r.cuelloBotella());
        assertEquals(1.33, r.balance());
        assertThat(r.fortalezas()).anyMatch(f -> f.contains("equilibrados")).anyMatch(f -> f.contains("doble canal"));
    }

    @Test
    void unaGraficaDebilParaElProcesadorEsElCuelloDeBotella() {
        var cpu = Datos.p(20, Categoria.PROCESADOR, "2900", 5, Map.of("socket", "LGA1700", "nucleos", "16", "rendimiento", "95"));
        var gpu = Datos.p(10, Categoria.TARJETA_GRAFICA, "2700", 7, Map.of("rendimiento", "55"));
        var r = analizador.analizar(piezas(cpu, gpu, Datos.RAM_32_DOS, Datos.SSD), UsoPrevisto.JUEGOS, SIN_PROBLEMAS);
        Hallazgo b = de(r, Tipo.BALANCE);
        assertEquals(Categoria.TARJETA_GRAFICA, b.categoria());
        assertEquals(30, b.puntos()); // 55/95 = 0,58, lejos del mínimo 0,9: llega al tope
        assertEquals("La tarjeta gráfica limita el rendimiento", r.cuelloBotella());
        assertThat(b.explicacion()).contains("0,6 veces").contains("entre 0,9 y 1,6");
    }

    @Test
    void unProcesadorDebilParaLaGraficaLimitaALaGrafica() {
        var cpu = Datos.p(40, Categoria.PROCESADOR, "700", 5, Map.of("nucleos", "6", "rendimiento", "45"));
        var r = analizador.analizar(piezas(cpu, Datos.GPU_7800, Datos.RAM_32_DOS, Datos.SSD), UsoPrevisto.JUEGOS, SIN_PROBLEMAS);
        Hallazgo b = de(r, Tipo.BALANCE);
        assertEquals(Categoria.PROCESADOR, b.categoria());
        assertEquals(22, b.puntos()); // 88/45 = 1,96 → (1,96/1,6 - 1) × 100
        assertEquals("El procesador limita a la tarjeta gráfica", r.cuelloBotella());
    }

    @Test
    void paraEdicionPideMasRamYNucleos() {
        var r = analizador.analizar(piezas(Datos.CPU_5600, Datos.RAM_16_UNO, Datos.GPU_4060, Datos.HDD), UsoPrevisto.EDICION,
                SIN_PROBLEMAS);
        assertEquals(16, de(r, Tipo.RAM).puntos());   // 16 de 32 GB
        assertEquals(10, de(r, Tipo.NUCLEOS).puntos()); // 6 de 8 núcleos
        assertEquals(8, de(r, Tipo.DOBLE_CANAL).puntos());
        assertEquals(8, de(r, Tipo.ALMACENAMIENTO).puntos());
        assertEquals(58, r.puntaje());
    }

    @Test
    void paraOficinaUnaGraficaPotenteEsGastoYNoSeEvaluaElBalance() {
        var r = analizador.analizar(piezas(Datos.CPU_5600, Datos.GPU_7800, Datos.RAM_32_DOS, Datos.SSD), UsoPrevisto.OFICINA,
                SIN_PROBLEMAS);
        assertEquals(10, de(r, Tipo.GASTO).puntos());
        assertThat(r.recomendaciones()).noneMatch(h -> h.tipo() == Tipo.BALANCE);
        assertNull(r.balance());
    }

    @Test
    void paraJuegosSinGraficaRestaYPideAgregarUna() {
        var r = analizador.analizar(piezas(Datos.CPU_5600, Datos.RAM_32_DOS, Datos.SSD), UsoPrevisto.JUEGOS, SIN_PROBLEMAS);
        Hallazgo h = de(r, Tipo.USO);
        assertEquals(30, h.puntos());
        assertEquals(Categoria.TARJETA_GRAFICA, h.categoria());
        var oficina = analizador.analizar(piezas(Datos.CPU_5600, Datos.RAM_32_DOS, Datos.SSD), UsoPrevisto.OFICINA, SIN_PROBLEMAS);
        assertThat(oficina.recomendaciones()).noneMatch(x -> x.tipo() == Tipo.USO);
    }

    @Test
    void lasIncompatibilidadesRestanYSonElPrimerProblema() {
        var reporte = new ReporteCompatibilidad(Severidad.INCOMPATIBLE, List.of(
                new ResultadoRegla(9L, "Conectores de la fuente", Severidad.INCOMPATIBLE, "Faltan conectores."),
                new ResultadoRegla(10L, "Consumo y fuente de poder", Severidad.ADVERTENCIA, "Poco margen.")), 428);
        var r = analizador.analizar(piezas(Datos.CPU_5700X, Datos.RAM_32_DOS, Datos.GPU_7800, Datos.SSD), UsoPrevisto.JUEGOS, reporte);
        assertEquals(30, de(r, Tipo.COMPATIBILIDAD).puntos()); // 25 + 5
        assertThat(r.cuelloBotella()).startsWith("Hay piezas incompatibles");
        assertEquals(40, r.puntaje()); // 100 - 30 = 70, pero con incompatibilidades el tope es 40
    }

    @Test
    void sinIndiceDeRendimientoAvisaYNoInventaElBalance() {
        var gpu = Datos.p(50, Categoria.TARJETA_GRAFICA, "2000", 3, Map.of("tdp_w", "150"));
        var r = analizador.analizar(piezas(Datos.CPU_5700X, gpu, Datos.RAM_32_DOS, Datos.SSD), UsoPrevisto.JUEGOS, SIN_PROBLEMAS);
        Hallazgo d = de(r, Tipo.DATOS);
        assertEquals(0, d.puntos());
        assertThat(d.explicacion()).contains("Producto 50");
        assertNull(r.balance());
    }

    @Test
    void sinProcesadorNoSeAnaliza() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> analizador.analizar(piezas(Datos.RAM_32_DOS), UsoPrevisto.JUEGOS, SIN_PROBLEMAS));
        assertThat(e.getMessage()).contains("procesador");
    }
}
