package com.orvex.catalogo.servicio;

import static com.orvex.catalogo.Datos.producto;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orvex.catalogo.Categoria;
import com.orvex.catalogo.modelo.EstadoProducto;
import com.orvex.catalogo.modelo.Producto;
import com.orvex.catalogo.modelo.Repositorios.ProductoRepository;
import com.orvex.catalogo.servicio.CatalogoService.Filtros;
import com.orvex.catalogo.servicio.CatalogoService.Orden;
import com.orvex.catalogo.servicio.InventarioCliente.Existencias;
import com.orvex.common.web.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogoServiceTest {

    ProductoRepository repo = mock(ProductoRepository.class);
    InventarioCliente inventario = mock(InventarioCliente.class);
    CatalogoService catalogo = new CatalogoService(repo, inventario);

    static final Producto R5 = producto(1, Categoria.PROCESADOR, "Ryzen 5 5600", "AMD", "1100", EstadoProducto.PUBLICADO,
            Map.of("socket", "AM4", "tdp_w", "65"));
    static final Producto I5 = producto(2, Categoria.PROCESADOR, "Core i5-12400F", "Intel", "1050", EstadoProducto.PUBLICADO,
            Map.of("socket", "LGA1700", "tdp_w", "65"));
    static final Producto R7 = producto(3, Categoria.PROCESADOR, "Ryzen 7 7700", "AMD", "2100", EstadoProducto.PUBLICADO,
            Map.of("socket", "AM5", "tdp_w", "65"));
    static final Producto B550 = producto(4, Categoria.PLACA_MADRE, "Placa B550M", "Gigabyte", "780", EstadoProducto.PUBLICADO,
            Map.of("socket", "AM4", "tipo_ram", "DDR4", "factor_forma", "mATX"));
    static final Producto COOLER = producto(5, Categoria.REFRIGERACION, "Cooler de torre", "DeepCool", "250",
            EstadoProducto.PUBLICADO, Map.of("altura_mm", "155", "sockets", "AM4,AM5,LGA1700"));
    static final Producto GPU = producto(6, Categoria.TARJETA_GRAFICA, "Tarjeta gráfica RTX 4060", "NVIDIA", "3300",
            EstadoProducto.PUBLICADO, Map.of("largo_mm", "240", "tdp_w", "115"));

    static final List<Producto> TODOS = List.of(R5, I5, R7, B550, COOLER, GPU);

    @BeforeEach
    void preparar() {
        when(repo.findByEstadoIn(any())).thenReturn(TODOS);
        when(inventario.existencias(anyCollection())).thenReturn(Optional.of(Map.of(
                1L, new Existencias(1L, 5, 0, 2),
                2L, new Existencias(2L, 0, 0, 2),
                3L, new Existencias(3L, 2, 1, 2))));
    }

    static Filtros filtros(Categoria c) {
        return new Filtros(null, c, null, null, null, null, null, false);
    }

    private List<Long> ids(Filtros f, Orden o) {
        return catalogo.buscar(f, o, 0, 12).resultados().elementos().stream().map(p -> p.id()).toList();
    }

    @Test
    void filtraPorCategoriaYOrdenaPorPrecio() {
        assertEquals(List.of(2L, 1L, 3L), ids(filtros(Categoria.PROCESADOR), Orden.PRECIO_ASC));
        assertEquals(List.of(3L, 1L, 2L), ids(filtros(Categoria.PROCESADOR), Orden.PRECIO_DESC));
    }

    @Test
    void buscaSinImportarTildesNiMayusculas() {
        var f = new Filtros("GRAFICA rtx", null, null, null, null, null, null, false);
        assertEquals(List.of(6L), ids(f, Orden.PRECIO_ASC));
    }

    @Test
    void elTextoTambienBuscaEnElNombreDeLaCategoria() {
        // "Placa B550M" no dice "madre", pero su categoría es "Placa madre".
        assertEquals(List.of(4L), ids(new Filtros("madre gigabyte", null, null, null, null, null, null, false), Orden.PRECIO_ASC));
        assertEquals(List.of(5L), ids(new Filtros("refrigeracion", null, null, null, null, null, null, false), Orden.PRECIO_ASC));
    }

    @Test
    void filtraPorMarcaPrecioYSocket() {
        assertEquals(List.of(1L, 3L), ids(new Filtros(null, Categoria.PROCESADOR, Set.of("AMD"), null, null, null, null, false), Orden.PRECIO_ASC));
        assertEquals(List.of(1L), ids(new Filtros(null, Categoria.PROCESADOR, null, new BigDecimal("1060"), new BigDecimal("2000"), null, null, false), Orden.PRECIO_ASC));
        // El socket también encuentra coolers que lo admiten en su lista de sockets.
        assertEquals(List.of(5L, 4L, 1L), ids(new Filtros(null, null, null, null, null, "AM4", null, false), Orden.PRECIO_ASC));
    }

    @Test
    void soloDisponiblesExcluyeLosQueNoTienenUnidades() {
        // El producto 2 tiene 0 y el 6 no tiene fila de stock: ninguno aparece.
        var f = new Filtros(null, null, null, null, null, null, null, true);
        assertEquals(List.of(1L, 3L), ids(f, Orden.PRECIO_ASC));
    }

    @Test
    void stockVieneDeInventarioDescontandoReservas() {
        var r = catalogo.buscar(filtros(Categoria.PROCESADOR), Orden.PRECIO_ASC, 0, 12);
        var porId = r.resultados().elementos().stream().collect(java.util.stream.Collectors.toMap(p -> p.id(), p -> p));
        assertEquals(0, porId.get(2L).stock());
        assertFalse(porId.get(2L).enStock());
        assertEquals(2, porId.get(3L).stock()); // la fila dice 2 vendibles (3 menos 1 reservada, calculado en inventario)
        assertTrue(r.disponibilidadConfirmada());
    }

    @Test
    void sinInventarioElCatalogoSigueFuncionandoConDisponibilidadPorConfirmar() {
        when(inventario.existencias(anyCollection())).thenReturn(Optional.empty());
        var r = catalogo.buscar(new Filtros(null, null, null, null, null, null, null, true), Orden.PRECIO_ASC, 0, 12);
        assertEquals(6, r.resultados().total()); // el filtro de disponibilidad no puede aplicarse: no oculta nada
        assertFalse(r.disponibilidadConfirmada());
        assertNull(r.resultados().elementos().get(0).stock());
        assertNull(r.resultados().elementos().get(0).enStock());
    }

    @Test
    void facetasSalenDeLaCategoriaElegida() {
        var f = catalogo.buscar(new Filtros(null, Categoria.PROCESADOR, Set.of("Intel"), null, null, null, null, false),
                Orden.PRECIO_ASC, 0, 12).facetas();
        assertEquals(List.of("AMD", "Intel"), f.marcas());
        assertEquals(List.of("AM4", "AM5", "LGA1700"), f.sockets());
        assertEquals(0, new BigDecimal("2100").compareTo(f.precioMaximo()));
    }

    @Test
    void paginaYRecortaElTamano() {
        var r = catalogo.buscar(filtros(null), Orden.PRECIO_ASC, 1, 4);
        assertEquals(2, r.resultados().totalPaginas());
        assertEquals(2, r.resultados().elementos().size());
        assertEquals(6, r.resultados().total());
        // Una página fuera de rango devuelve la última, no una lista vacía.
        assertEquals(1, catalogo.buscar(filtros(null), Orden.PRECIO_ASC, 99, 4).resultados().pagina());
    }

    @Test
    void detalleIncluyeRelacionadosDeLaMismaCategoriaPorPrecioCercano() {
        when(repo.findWithEspecificacionesById(1L)).thenReturn(Optional.of(R5));
        var d = catalogo.detalle(1L);
        assertEquals(1L, d.producto().id());
        assertEquals(List.of(2L, 3L), d.relacionados().stream().map(p -> p.id()).toList());
    }

    @Test
    void detalleDeProductoNoPublicadoDa404() {
        var borrador = producto(9, Categoria.RAM, "RAM en preparación", null, "300", EstadoProducto.BORRADOR, Map.of());
        when(repo.findWithEspecificacionesById(9L)).thenReturn(Optional.of(borrador));
        var e = assertThrows(ApiException.class, () -> catalogo.detalle(9L));
        assertEquals(404, e.getEstado().value());
    }

    @Test
    void categoriasCuentanProductosYPrecioDesde() {
        var c = catalogo.categorias();
        assertEquals(8, c.size());
        var cpu = c.stream().filter(x -> x.id() == Categoria.PROCESADOR).findFirst().orElseThrow();
        assertEquals(3, cpu.productos());
        assertEquals(0, new BigDecimal("1050").compareTo(cpu.desde()));
        assertNull(c.stream().filter(x -> x.id() == Categoria.GABINETE).findFirst().orElseThrow().desde());
    }
}
