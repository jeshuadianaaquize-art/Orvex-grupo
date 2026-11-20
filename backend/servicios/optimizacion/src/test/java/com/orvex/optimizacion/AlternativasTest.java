package com.orvex.optimizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orvex.compat.Categoria;
import com.orvex.compat.Severidad;
import com.orvex.optimizacion.analisis.ParametrosOptimizacion;
import com.orvex.optimizacion.analisis.UsoPrevisto;
import com.orvex.optimizacion.clientes.CatalogoCliente;
import com.orvex.optimizacion.clientes.CatalogoCliente.Producto;
import com.orvex.optimizacion.clientes.CompatibilidadCliente;
import com.orvex.optimizacion.modelo.AnalisisRepository;
import com.orvex.optimizacion.servicio.OptimizacionService;
import com.orvex.optimizacion.servicio.OptimizacionService.Alternativa;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Alternativas (RF-10) */
class AlternativasTest {

    private OptimizacionService servicio;

    @BeforeEach
    void preparar() {
        var catalogo = mock(CatalogoCliente.class);
        var compat = mock(CompatibilidadCliente.class);
        when(catalogo.publicados()).thenReturn(Datos.CATALOGO);
        when(compat.reglas()).thenReturn(Datos.reglas());
        servicio = new OptimizacionService(catalogo, compat, mock(AnalisisRepository.class), ParametrosOptimizacion.propuestos(),
                new ObjectMapper(), Clock.systemUTC());
    }

    private static List<Long> ids(Producto... ps) {
        return Arrays.stream(ps).map(Producto::id).toList();
    }

    @Test
    void unaIncompatibilidadPideAlternativasQueLaResuelvan() {
        var a = servicio.analizar(ids(Datos.CPU_5600, Datos.PLACA_AM4, Datos.RAM_32_DOS, Datos.GPU_7800, Datos.SSD, Datos.FTE_450,
                Datos.GABINETE), UsoPrevisto.JUEGOS);
        assertEquals(Severidad.INCOMPATIBLE, a.compatibilidad());
        assertTrue(a.buscoAlternativas());
        List<Alternativa> fuentes = a.alternativas().stream().filter(x -> x.categoria() == Categoria.FUENTE).toList();
        assertThat(fuentes).extracting(x -> x.producto().id()).containsExactly(Datos.FTE_650.id(), Datos.FTE_750.id());
        assertThat(fuentes.get(0).motivo()).startsWith("Resuelve: Conectores de la fuente");
        assertEquals(0, fuentes.get(0).diferenciaPrecio().compareTo(new java.math.BigDecimal("100")));
        assertEquals(Datos.FTE_450.id(), fuentes.get(0).reemplaza().id());
        // También sirve una gráfica con un solo conector, que además entra en la fuente de 450 W.
        assertThat(a.alternativas()).anyMatch(x -> x.producto().id().equals(Datos.GPU_4060.id()));
        assertThat(a.alternativas()).allMatch(x -> x.puntajeNuevo() > a.puntaje());
    }

    @Test
    void conPuntajeBajoProponeMejorasEnStockQueSiguenSiendoCompatibles() {
        var a = servicio.analizar(ids(Datos.CPU_5600, Datos.PLACA_AM4, Datos.RAM_16_UNO, Datos.GPU_4060, Datos.HDD, Datos.FTE_650,
                Datos.GABINETE), UsoPrevisto.EDICION);
        assertEquals(58, a.puntaje());
        assertTrue(a.buscoAlternativas());
        var porProducto = a.alternativas().stream().map(x -> x.producto().id()).toList();
        assertThat(porProducto).contains(Datos.RAM_32_DOS.id(), Datos.SSD.id(), Datos.CPU_5700X.id())
                .doesNotContain(Datos.RAM_32_SIN_STOCK.id()) // sin stock
                .doesNotContain(Datos.CPU_7600.id());        // socket AM5: no entra en la placa AM4
        var ram = a.alternativas().stream().filter(x -> x.categoria() == Categoria.RAM).findFirst().orElseThrow();
        assertEquals(82, ram.puntajeNuevo()); // ya no resta por capacidad ni por un solo canal
        assertEquals("Sube el puntaje de 58 a 82.", ram.motivo());
    }

    @Test
    void unArmadoBienPuntuadoNoBuscaAlternativas() {
        var a = servicio.analizar(ids(Datos.CPU_5700X, Datos.PLACA_AM4, Datos.RAM_32_DOS, Datos.GPU_7800, Datos.SSD, Datos.FTE_750,
                Datos.GABINETE), UsoPrevisto.JUEGOS);
        assertEquals(100, a.puntaje());
        assertFalse(a.buscoAlternativas());
        assertThat(a.alternativas()).isEmpty();
    }

    @Test
    void productosInexistentesORepetidosPorCategoriaSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> servicio.analizar(List.of(1L, 999L), UsoPrevisto.JUEGOS));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.analizar(ids(Datos.CPU_5600, Datos.CPU_5700X), UsoPrevisto.JUEGOS));
    }
}
