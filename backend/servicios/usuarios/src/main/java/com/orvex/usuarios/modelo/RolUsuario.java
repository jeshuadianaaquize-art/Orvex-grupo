package com.orvex.usuarios.modelo;

import com.orvex.seguridad.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla "rol". Los cuatro roles se cargan en la migración V1. */
@Entity
@Table(name = "rol")
public class RolUsuario {

    @Id
    @Column(name = "id_rol")
    private Short id;

    @Column(nullable = false, unique = true, length = 20)
    private String nombre;

    @Column(length = 100)
    private String descripcion;

    protected RolUsuario() {
    }

    public Short getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }

    public Rol aRol() {
        return Rol.desde(nombre);
    }
}
