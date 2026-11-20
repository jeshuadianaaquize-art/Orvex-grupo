package com.orvex.catalogo.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla "proveedor": de quién llega la mercadería en un ingreso de inventario (RF-14). */
@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 100)
    private String contacto;

    protected Proveedor() {
    }

    public Proveedor(String nombre, String contacto) {
        actualizar(nombre, contacto);
    }

    public final void actualizar(String nombre, String contacto) {
        this.nombre = nombre;
        this.contacto = contacto;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getContacto() { return contacto; }
}
