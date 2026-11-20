package com.orvex.catalogo.web;

import com.orvex.catalogo.Categoria;
import com.orvex.catalogo.modelo.Producto;
import com.orvex.catalogo.servicio.InventarioCliente.Existencias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Contratos JSON del servicio de catálogo. */
public final class Dtos {

    private Dtos() {
    }

    /** Producto tal como lo ve la tienda */
    public record ProductoDto(Long id, String sku, String nombre, String marca, BigDecimal precio, String fotoUrl,
                              Categoria categoria, Map<String, String> specs, Integer stock, Boolean enStock) {

        public static ProductoDto de(Producto p, Map<Long, Existencias> existencias) {
            Integer stock = null;
            if (existencias != null) {
                Existencias e = existencias.get(p.getId());
                stock = e == null ? 0 : e.vendibles();
            }
            return new ProductoDto(p.getId(), p.getSku(), p.getNombre(), p.getMarca(), p.getPrecio(), p.getFotoUrl(),
                    p.getCategoria().codigo(), p.specs(), stock, stock == null ? null : stock > 0);
        }
    }

    public record Facetas(List<String> marcas, List<String> sockets, List<String> tiposRam, BigDecimal precioMaximo) {
    }

    public record Pagina<T>(List<T> elementos, int pagina, int totalPaginas, long total) {
    }

    public record Busqueda(Pagina<ProductoDto> resultados, Facetas facetas, boolean disponibilidadConfirmada) {
    }

    public record Detalle(ProductoDto producto, List<ProductoDto> relacionados) {
    }

    public record CategoriaDto(Categoria id, String nombre, long productos, BigDecimal desde) {
    }

    // administración

    public record EspecificacionEntrada(
            @NotBlank(message = "falta el nombre")
            @Pattern(regexp = "^[a-z][a-z0-9_]{0,39}$", message = "usa minúsculas, números y guion bajo (ej.: tdp_w)")
            String nombre,
            @NotBlank(message = "falta el valor") @Size(max = 80, message = "máximo 80 caracteres") String valor) {
    }

    public record ProductoEntrada(
            @NotNull(message = "elige la categoría") Categoria categoria,
            @NotBlank(message = "escribe el SKU") @Size(max = 30, message = "máximo 30 caracteres")
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "solo letras, números, punto, guion y guion bajo") String sku,
            @NotBlank(message = "escribe el nombre") @Size(max = 120, message = "máximo 120 caracteres") String nombre,
            @Size(max = 40, message = "máximo 40 caracteres") String marca,
            @NotNull(message = "escribe el precio")
            @DecimalMin(value = "0.00", message = "no puede ser negativo")
            @DecimalMax(value = "99999999.99", message = "es demasiado alto")
            @Digits(integer = 8, fraction = 2, message = "máximo 2 decimales") BigDecimal precio,
            @Size(max = 40, message = "máximo 40 especificaciones") List<@Valid EspecificacionEntrada> especificaciones) {
    }

    public record CambioEstado(@NotNull(message = "falta el estado") String estado) {
    }

    public record ProductoAdminDto(Long id, String sku, String nombre, String marca, BigDecimal precio, String fotoUrl,
                                   Categoria categoria, String estado, Map<String, String> specs, List<String> faltantes) {
    }

    public record SpecDto(String clave, String etiqueta, Categoria.Tipo tipo, String unidad, boolean requerida) {
        public static SpecDto de(Categoria.Spec s) {
            return new SpecDto(s.clave(), s.etiqueta(), s.tipo(), s.unidad(), s.requerida());
        }
    }

    public record CategoriaSpecsDto(Categoria categoria, String nombre, List<SpecDto> especificaciones) {
    }
}
