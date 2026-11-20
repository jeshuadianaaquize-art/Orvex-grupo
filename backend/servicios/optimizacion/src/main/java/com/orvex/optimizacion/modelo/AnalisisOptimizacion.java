package com.orvex.optimizacion.modelo;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Tablas "analisis_optimizacion" */
@Entity
@Table(name = "analisis_optimizacion")
public class AnalisisOptimizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_analisis")
    private Long id;

    @Column(name = "id_configuracion", nullable = false, unique = true, updatable = false)
    private Long configuracionId;

    @Column(nullable = false)
    private int puntaje;

    @Column(name = "cuello_botella", length = 100)
    private String cuelloBotella;

    /** Hallazgos en JSON, con su explicación: se muestran igual que en el análisis en vivo. */
    @Column(columnDefinition = "TEXT")
    private String recomendaciones;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ElementCollection
    @CollectionTable(name = "alternativa_sugerida", joinColumns = @JoinColumn(name = "id_analisis"))
    @Column(name = "id_producto", nullable = false)
    private Set<Long> alternativas = new LinkedHashSet<>();

    protected AnalisisOptimizacion() {
    }

    public AnalisisOptimizacion(Long configuracionId) {
        this.configuracionId = configuracionId;
    }

    public void actualizar(int puntaje, String cuelloBotella, String recomendaciones, LocalDateTime fecha, List<Long> alternativas) {
        this.puntaje = puntaje;
        this.cuelloBotella = cuelloBotella;
        this.recomendaciones = recomendaciones;
        this.fecha = fecha;
        this.alternativas.clear();
        this.alternativas.addAll(alternativas);
    }

    public Long getId() { return id; }
    public Long getConfiguracionId() { return configuracionId; }
    public int getPuntaje() { return puntaje; }
    public String getCuelloBotella() { return cuelloBotella; }
    public String getRecomendaciones() { return recomendaciones; }
    public LocalDateTime getFecha() { return fecha; }
    public Set<Long> getAlternativas() { return alternativas; }
}
