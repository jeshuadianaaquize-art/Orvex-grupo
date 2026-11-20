package com.orvex.usuarios.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla "direccion": direcciones de entrega de cada usuario (RF-02). */
@Entity
@Table(name = "direccion")
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion")
    private Long id;

    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 120)
    private String calle;

    @Column(nullable = false, length = 50)
    private String ciudad;

    @Column(length = 150)
    private String referencia;

    protected Direccion() {
    }

    public Direccion(Long usuarioId, String calle, String ciudad, String referencia) {
        this.usuarioId = usuarioId;
        actualizar(calle, ciudad, referencia);
    }

    public void actualizar(String calle, String ciudad, String referencia) {
        this.calle = calle;
        this.ciudad = ciudad;
        this.referencia = referencia;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getCalle() { return calle; }
    public String getCiudad() { return ciudad; }
    public String getReferencia() { return referencia; }
}
