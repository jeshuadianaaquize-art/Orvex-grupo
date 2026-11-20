package com.orvex.compatibilidad.modelo;

import com.orvex.compat.Categoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla "regla_compatibilidad" (RNF-06: las reglas se cambian sin tocar el código). */
@Entity
@Table(name = "regla_compatibilidad")
public class ReglaCompatibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Long id;

    @Column(name = "id_categoria_a", nullable = false)
    private short categoriaA;

    @Column(name = "id_categoria_b", nullable = false)
    private short categoriaB;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 255)
    private String condicion;

    protected ReglaCompatibilidad() {
    }

    public ReglaCompatibilidad(Categoria a, Categoria b, String nombre, String condicion) {
        actualizar(a, b, nombre, condicion);
    }

    public final void actualizar(Categoria a, Categoria b, String nombre, String condicion) {
        this.categoriaA = idDe(a);
        this.categoriaB = idDe(b);
        this.nombre = nombre;
        this.condicion = condicion;
    }

    /** La tabla categoria del catálogo y el enum Categoria están en el mismo orden (id = ordinal + 1). */
    public static short idDe(Categoria c) {
        return (short) (c.ordinal() + 1);
    }

    public static Categoria categoria(short id) {
        if (id < 1 || id > Categoria.values().length) {
            throw new IllegalStateException("Categoría desconocida: " + id);
        }
        return Categoria.values()[id - 1];
    }

    public Long getId() { return id; }
    public Categoria getCategoriaA() { return categoria(categoriaA); }
    public Categoria getCategoriaB() { return categoria(categoriaB); }
    public String getNombre() { return nombre; }
    public String getCondicion() { return condicion; }
}
