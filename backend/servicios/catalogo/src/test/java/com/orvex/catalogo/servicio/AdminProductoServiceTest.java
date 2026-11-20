package com.orvex.catalogo.servicio;

import static com.orvex.catalogo.Datos.categoria;
import static com.orvex.catalogo.Datos.producto;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orvex.catalogo.Categoria;
import com.orvex.catalogo.modelo.EstadoProducto;
import com.orvex.catalogo.modelo.Producto;
import com.orvex.catalogo.modelo.Repositorios.CategoriaRepository;
import com.orvex.catalogo.modelo.Repositorios.ProductoRepository;
import com.orvex.catalogo.web.Dtos;
import com.orvex.catalogo.web.Dtos.EspecificacionEntrada;
import com.orvex.common.web.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminProductoServiceTest {

    ProductoRepository productos = mock(ProductoRepository.class);
    CategoriaRepository categorias = mock(CategoriaRepository.class);
    AdminProductoService admin = new AdminProductoService(productos, categorias, mock(com.orvex.eventos.PublicadorEventos.class));

    @BeforeEach
    void preparar() {
        for (Categoria c : Categoria.values()) {
            when(categorias.findByNombre(c.nombreBd())).thenReturn(Optional.of(categoria(c)));
        }
        when(productos.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    static Dtos.ProductoEntrada cpu(String sku, String precio, List<EspecificacionEntrada> specs) {
        return new Dtos.ProductoEntrada(Categoria.PROCESADOR, sku, "Procesador nuevo", "AMD", new BigDecimal(precio), specs);
    }

    @Test
    void productoNuevoNaceEnBorradorConSkuEnMayusculas() {
        var r = admin.crear(1L, cpu("cpu-nuevo", "999.90", List.of(new EspecificacionEntrada("socket", "AM5"))));
        assertEquals("BORRADOR", r.estado());
        assertEquals("CPU-NUEVO", r.sku());
        assertEquals(List.of("Consumo (TDP)"), r.faltantes());
    }

    @Test
    void skuRepetidoDaConflictoEnElCampo() {
        when(productos.existsBySkuIgnoreCase("CPU-1")).thenReturn(true);
        var e = assertThrows(ApiException.class, () -> admin.crear(1L, cpu("cpu-1", "10", List.of())));
        assertEquals(409, e.getEstado().value());
        assertTrue(e.getCampos().containsKey("sku"));
    }

    @Test
    void especificacionNumericaConTextoSeRechaza() {
        var e = assertThrows(ApiException.class, () -> admin.crear(1L,
                cpu("cpu-2", "10", List.of(new EspecificacionEntrada("tdp_w", "65 vatios")))));
        assertEquals("Consumo (TDP) debe ser un número", e.getCampos().get("especificaciones.tdp_w"));
    }

    @Test
    void normalizaNumerosConComaYListasConEspacios() {
        var specs = AdminProductoService.validarEspecificaciones(Categoria.GABINETE, List.of(
                new EspecificacionEntrada("max_largo_gpu_mm", "330,5"),
                new EspecificacionEntrada("factores_forma", " ATX , mATX,,ATX ")));
        assertEquals("330.5", specs.get("max_largo_gpu_mm"));
        assertEquals("ATX,mATX", specs.get("factores_forma"));
    }

    @Test
    void especificacionRepetidaSeRechaza() {
        var e = assertThrows(ApiException.class, () -> AdminProductoService.validarEspecificaciones(Categoria.RAM,
                List.of(new EspecificacionEntrada("tipo_ram", "DDR4"), new EspecificacionEntrada("tipo_ram", "DDR5"))));
        assertTrue(e.getCampos().containsKey("especificaciones.tipo_ram"));
    }

    @Test
    void noSePublicaSinLasEspecificacionesQueUsaLaCompatibilidad() {
        Producto p = producto(10, Categoria.PROCESADOR, "CPU", "AMD", "100", EstadoProducto.BORRADOR, Map.of("socket", "AM4"));
        when(productos.findWithEspecificacionesById(10L)).thenReturn(Optional.of(p));
        var e = assertThrows(ApiException.class, () -> admin.cambiarEstado(1L, 10L, "publicado"));
        assertTrue(e.getMessage().contains("Consumo (TDP)"));
        assertEquals(EstadoProducto.BORRADOR, p.getEstado());
    }

    @Test
    void transicionesValidasDelCicloDeVida() {
        Producto p = producto(11, Categoria.RAM, "RAM", "Kingston", "380", EstadoProducto.BORRADOR, Map.of("tipo_ram", "DDR4"));
        when(productos.findWithEspecificacionesById(11L)).thenReturn(Optional.of(p));
        assertEquals("PUBLICADO", admin.cambiarEstado(1L, 11L, "PUBLICADO").estado());
        assertEquals("INACTIVO", admin.cambiarEstado(1L, 11L, "inactivo").estado());
        assertEquals("PUBLICADO", admin.cambiarEstado(1L, 11L, "publicado").estado());
    }

    @Test
    void transicionInvalidaDaConflicto() {
        Producto p = producto(12, Categoria.RAM, "RAM", "Kingston", "380", EstadoProducto.PUBLICADO, Map.of("tipo_ram", "DDR4"));
        when(productos.findWithEspecificacionesById(12L)).thenReturn(Optional.of(p));
        var e = assertThrows(ApiException.class, () -> admin.cambiarEstado(1L, 12L, "BORRADOR"));
        assertEquals(409, e.getEstado().value());
        assertThrows(ApiException.class, () -> admin.cambiarEstado(1L, 12L, "ELIMINADO"));
    }

    @Test
    void productoPublicadoNoPuedeQuedarIncompletoAlEditarlo() {
        Producto p = producto(13, Categoria.RAM, "RAM", "Kingston", "380", EstadoProducto.PUBLICADO, Map.of("tipo_ram", "DDR4"));
        when(productos.findWithEspecificacionesById(13L)).thenReturn(Optional.of(p));
        var sinTipo = new Dtos.ProductoEntrada(Categoria.RAM, "RAM-1", "RAM", "Kingston", new BigDecimal("400"), List.of());
        assertThrows(ApiException.class, () -> admin.actualizar(1L, 13L, sinTipo));
        assertEquals(0, new BigDecimal("380").compareTo(p.getPrecio()));
    }

    @Test
    void editarCambiaPrecioYEspecificaciones() {
        Producto p = producto(14, Categoria.RAM, "RAM", "Kingston", "380", EstadoProducto.BORRADOR, Map.of("tipo_ram", "DDR4"));
        when(productos.findWithEspecificacionesById(14L)).thenReturn(Optional.of(p));
        var r = admin.actualizar(1L, 14L, new Dtos.ProductoEntrada(Categoria.RAM, "ram-1", "RAM 16 GB", "Kingston",
                new BigDecimal("399.90"), List.of(new EspecificacionEntrada("tipo_ram", "DDR5"),
                new EspecificacionEntrada("capacidad_gb", "16"))));
        assertEquals(0, new BigDecimal("399.90").compareTo(r.precio()));
        assertEquals(Map.of("tipo_ram", "DDR5", "capacidad_gb", "16"), r.specs());
    }
}
