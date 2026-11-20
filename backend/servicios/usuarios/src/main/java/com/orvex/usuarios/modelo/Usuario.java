package com.orvex.usuarios.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Tabla "usuario". La contraseña solo se guarda como hash bcrypt (RNF-04). */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private RolUsuario rol;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, columnDefinition = "ENUM('activo','bloqueado')")
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    protected Usuario() {
    }

    public Usuario(RolUsuario rol, String nombre, String correo, String contrasenaHash, String telefono) {
        this.rol = rol;
        this.nombre = nombre;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.telefono = telefono;
    }

    public Long getId() { return id; }
    public RolUsuario getRol() { return rol; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getContrasenaHash() { return contrasenaHash; }
    public String getTelefono() { return telefono; }
    public EstadoUsuario getEstado() { return estado; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }

    public boolean estaActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public void actualizarPerfil(String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }

    public void cambiarContrasenaHash(String hash) {
        this.contrasenaHash = hash;
    }

    public void cambiarRol(RolUsuario rol) {
        this.rol = rol;
    }

    public void cambiarEstado(EstadoUsuario estado) {
        this.estado = estado;
    }
}
