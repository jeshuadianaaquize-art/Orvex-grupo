package com.orvex.catalogo.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tabla "producto" del diccionario. Las especificaciones viven en su propia tabla. */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaEntidad categoria;

    @Column(nullable = false, unique = true, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 40)
    private String marca;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "foto_url", length = 255)
    private String fotoUrl;

    @Column(nullable = false, columnDefinition = "ENUM('borrador','publicado','inactivo')")
    private EstadoProducto estado = EstadoProducto.BORRADOR;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<EspecificacionTecnica> especificaciones = new ArrayList<>();

    protected Producto() {
    }

    public Producto(CategoriaEntidad categoria, String sku, String nombre, String marca, BigDecimal precio) {
        this.categoria = categoria;
        actualizar(sku, nombre, marca, precio);
    }

    public final void actualizar(String sku, String nombre, String marca, BigDecimal precio) {
        this.sku = sku;
        this.nombre = nombre;
        this.marca = marca;
        this.precio = precio;
    }

    public void cambiarCategoria(CategoriaEntidad categoria) {
        this.categoria = categoria;
    }

    public void cambiarFoto(String url) {
        this.fotoUrl = url;
    }

    public void cambiarEstado(EstadoProducto estado) {
        this.estado = estado;
    }

    /** Reemplaza las especificaciones conservando las filas que no cambian */
    public void reemplazarEspecificaciones(Map<String, String> nuevas) {
        especificaciones.removeIf(e -> !nuevas.containsKey(e.getNombre()));
        Map<String, EspecificacionTecnica> actuales = new LinkedHashMap<>();
        especificaciones.forEach(e -> actuales.put(e.getNombre(), e));
        nuevas.forEach((nombre, valor) -> {
            EspecificacionTecnica e = actuales.get(nombre);
            if (e == null) {
                especificaciones.add(new EspecificacionTecnica(this, nombre, valor));
            } else {
                e.cambiarValor(valor);
            }
        });
    }

    public Map<String, String> specs() {
        Map<String, String> m = new LinkedHashMap<>();
        especificaciones.forEach(e -> m.put(e.getNombre(), e.getValor()));
        return m;
    }

    public Long getId() { return id; }
    public CategoriaEntidad getCategoria() { return categoria; }
    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public String getMarca() { return marca; }
    public BigDecimal getPrecio() { return precio; }
    public String getFotoUrl() { return fotoUrl; }
    public EstadoProducto getEstado() { return estado; }
}
