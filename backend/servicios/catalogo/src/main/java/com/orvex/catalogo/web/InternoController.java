package com.orvex.catalogo.web;

import com.orvex.catalogo.modelo.Repositorios.ProductoRepository;
import com.orvex.catalogo.modelo.Repositorios.ProveedorRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** API interna para otros servicios (inventario) */
@RestController
public class InternoController {

    public record ProductoBasico(Long id, String sku, String nombre, String marca, String categoria, String estado,
                                 java.math.BigDecimal precio) {
    }

    private final ProductoRepository productos;
    private final ProveedorRepository proveedores;

    public InternoController(ProductoRepository productos, ProveedorRepository proveedores) {
        this.productos = productos;
        this.proveedores = proveedores;
    }

    @GetMapping("/internal/productos")
    @Transactional(readOnly = true)
    public List<ProductoBasico> productos() {
        return productos.findAll().stream()
                .map(p -> new ProductoBasico(p.getId(), p.getSku(), p.getNombre(), p.getMarca(),
                        p.getCategoria().codigo().name(), p.getEstado().name(), p.getPrecio()))
                .toList();
    }

    @GetMapping("/internal/productos/{id}")
    public ResponseEntity<Void> producto(@PathVariable Long id) {
        return productos.existsById(id) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    public record ProveedorBasico(Long id, String nombre) {
    }

    /** Nombres de los proveedores (el inventario los muestra en los pedidos sugeridos). */
    @GetMapping("/internal/proveedores")
    public List<ProveedorBasico> proveedores() {
        return proveedores.findAll().stream().map(p -> new ProveedorBasico(p.getId(), p.getNombre())).toList();
    }

    @GetMapping("/internal/proveedores/{id}")
    public ResponseEntity<Void> proveedor(@PathVariable Long id) {
        return proveedores.existsById(id) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
