package com.orvex.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Tabla "auditoria" (diccionario, tabla 5): una acción crítica con su usuario y fecha (RF-26). */
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @Column(name = "id_usuario", updatable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 60, updatable = false)
    private String accion;

    @Column(name = "entidad_afectada", nullable = false, length = 40, updatable = false)
    private String entidad;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String detalle;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(name = "id_evento", length = 36, updatable = false)
    private String eventoId;

    protected Auditoria() {
    }

    public Auditoria(Long usuarioId, String accion, String entidad, String detalle, LocalDateTime fecha, String eventoId) {
        this.usuarioId = usuarioId;
        this.accion = accion;
        this.entidad = entidad;
        this.detalle = detalle;
        this.fecha = fecha;
        this.eventoId = eventoId;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getAccion() { return accion; }
    public String getEntidad() { return entidad; }
    public String getDetalle() { return detalle; }
    public LocalDateTime getFecha() { return fecha; }
    public String getEventoId() { return eventoId; }
}
