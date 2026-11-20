package com.orvex.catalogo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Tabla "especificacion_tecnica" */
@Entity
@Table(name = "especificacion_tecnica")
public class EspecificacionTecnica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String valor;

    protected EspecificacionTecnica() {
    }

    EspecificacionTecnica(Producto producto, String nombre, String valor) {
        this.producto = producto;
        this.nombre = nombre;
        this.valor = valor;
    }

    public String getNombre() { return nombre; }
    public String getValor() { return valor; }

    void cambiarValor(String valor) {
        this.valor = valor;
    }
}
