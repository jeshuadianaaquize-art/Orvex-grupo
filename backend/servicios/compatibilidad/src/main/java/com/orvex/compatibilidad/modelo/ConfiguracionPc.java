package com.orvex.compatibilidad.modelo;

import com.orvex.compat.Severidad;
import com.orvex.compatibilidad.modelo.Modelo.UsoPrevisto;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Tablas "configuracion_pc", "item_configuracion" y "resultado_compatibilidad" */
@Entity
@Table(name = "configuracion_pc")
public class ConfiguracionPc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Long id;

    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "uso_previsto", columnDefinition = "ENUM('juegos','edicion','oficina')")
    private UsoPrevisto uso;

    @Column(name = "consumo_estimado_w")
    private Integer consumoEstimadoW;

    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal = BigDecimal.ZERO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "configuracion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Item> items = new ArrayList<>();

    @OneToMany(mappedBy = "configuracion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Resultado> resultados = new ArrayList<>();

    protected ConfiguracionPc() {
    }

    public ConfiguracionPc(Long usuarioId, LocalDateTime fecha) {
        this.usuarioId = usuarioId;
        this.fechaCreacion = fecha;
    }

    public void actualizar(String nombre, UsoPrevisto uso, List<Long> productos, Integer consumo, BigDecimal total,
                           List<Resultado.Datos> resultados) {
        this.nombre = nombre;
        this.uso = uso;
        this.consumoEstimadoW = consumo;
        this.precioTotal = total;
        this.items.clear();
        productos.forEach(p -> this.items.add(new Item(this, p)));
        this.resultados.clear();
        resultados.forEach(r -> this.resultados.add(new Resultado(this, r)));
    }

    /** El peor resultado guardado */
    public Severidad estado() {
        return resultados.stream().map(Resultado::getResultado).max(Comparator.naturalOrder()).orElse(Severidad.ADVERTENCIA);
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getNombre() { return nombre; }
    public UsoPrevisto getUso() { return uso; }
    public Integer getConsumoEstimadoW() { return consumoEstimadoW; }
    public BigDecimal getPrecioTotal() { return precioTotal; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public List<Item> getItems() { return items; }
    public List<Resultado> getResultados() { return resultados; }

    @Entity(name = "ItemConfiguracion")
    @Table(name = "item_configuracion")
    public static class Item {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id_item_config")
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "id_configuracion", nullable = false)
        private ConfiguracionPc configuracion;

        @Column(name = "id_producto", nullable = false)
        private Long productoId;

        @Column(nullable = false)
        private int cantidad = 1;

        protected Item() {
        }

        Item(ConfiguracionPc c, Long productoId) {
            this.configuracion = c;
            this.productoId = productoId;
        }

        public Long getProductoId() { return productoId; }
        public int getCantidad() { return cantidad; }
    }

    @Entity(name = "ResultadoCompatibilidad")
    @Table(name = "resultado_compatibilidad")
    public static class Resultado {
        public record Datos(Long reglaId, Severidad resultado, String mensaje) {
        }

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id_resultado")
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "id_configuracion", nullable = false)
        private ConfiguracionPc configuracion;

        @Column(name = "id_regla", nullable = false)
        private Long reglaId;

        @Column(nullable = false, columnDefinition = "ENUM('compatible','advertencia','incompatible')")
        private Severidad resultado;

        @Column(length = 255)
        private String mensaje;

        protected Resultado() {
        }

        Resultado(ConfiguracionPc c, Datos d) {
            this.configuracion = c;
            this.reglaId = d.reglaId();
            this.resultado = d.resultado();
            this.mensaje = d.mensaje() == null || d.mensaje().length() <= 255 ? d.mensaje() : d.mensaje().substring(0, 252) + "…";
        }

        public Long getReglaId() { return reglaId; }
        public Severidad getResultado() { return resultado; }
        public String getMensaje() { return mensaje; }
    }
}
