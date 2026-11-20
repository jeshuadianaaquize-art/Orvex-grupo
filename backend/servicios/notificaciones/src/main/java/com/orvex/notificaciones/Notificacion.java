package com.orvex.notificaciones;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;

/** Tabla "notificacion" (diccionario, tabla 4). */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    public enum Tipo { BIENVENIDA, RECUPERACION, PAGO, ESTADO_PEDIDO, STOCK_BAJO }

    public enum Estado { PENDIENTE, ENVIADO, FALLIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, updatable = false)
    private Tipo tipo;

    @Column(nullable = false, length = 255, updatable = false)
    private String mensaje;

    /** Diccionario: "fecha y hora de generación" */
    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "estado_envio", nullable = false)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "id_evento", length = 36, updatable = false)
    private String eventoId;

    protected Notificacion() {
    }

    public Notificacion(Long usuarioId, Tipo tipo, String mensaje, LocalDateTime fecha, String eventoId) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.mensaje = mensaje.length() > 255 ? mensaje.substring(0, 254) + "…" : mensaje;
        this.fecha = fecha;
        this.eventoId = eventoId;
    }

    public void enviada(LocalDateTime cuando) {
        this.estado = Estado.ENVIADO;
        this.fecha = cuando;
    }

    public void fallida() {
        this.estado = Estado.FALLIDO;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public Tipo getTipo() { return tipo; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFecha() { return fecha; }
    public Estado getEstado() { return estado; }
    public String getEventoId() { return eventoId; }

    abstract static class Minusculas<E extends Enum<E>> implements AttributeConverter<E, String> {
        private final Class<E> clase;

        Minusculas(Class<E> clase) {
            this.clase = clase;
        }

        @Override
        public String convertToDatabaseColumn(E e) {
            return e == null ? null : e.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public E convertToEntityAttribute(String v) {
            return v == null ? null : Enum.valueOf(clase, v.toUpperCase(Locale.ROOT));
        }
    }

    @Converter(autoApply = true)
    public static class TipoConv extends Minusculas<Tipo> {
        public TipoConv() {
            super(Tipo.class);
        }
    }

    @Converter(autoApply = true)
    public static class EstadoConv extends Minusculas<Estado> {
        public EstadoConv() {
            super(Estado.class);
        }
    }
}
