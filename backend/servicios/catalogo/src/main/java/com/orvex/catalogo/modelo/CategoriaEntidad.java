package com.orvex.catalogo.modelo;

import com.orvex.catalogo.Categoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla "categoria". Las 8 filas se cargan en la migración V4. */
@Entity
@Table(name = "categoria")
public class CategoriaEntidad {

    @Id
    @Column(name = "id_categoria")
    private Short id;

    @Column(nullable = false, unique = true, length = 40)
    private String nombre;

    protected CategoriaEntidad() {
    }

    public Short getId() { return id; }
    public String getNombre() { return nombre; }

    public Categoria codigo() {
        return Categoria.desdeBd(nombre);
    }
}
