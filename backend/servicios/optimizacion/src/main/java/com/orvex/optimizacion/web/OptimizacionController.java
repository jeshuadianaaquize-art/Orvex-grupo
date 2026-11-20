package com.orvex.optimizacion.web;

import com.orvex.common.web.Identidad;
import com.orvex.optimizacion.analisis.UsoPrevisto;
import com.orvex.optimizacion.servicio.OptimizacionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Optimización del armado (M05): puntaje, recomendaciones explicadas y alternativas en stock. */
@RestController
@RequestMapping("/api/optimizacion")
public class OptimizacionController {

    public record Solicitud(
            @NotNull(message = "falta la lista de productos")
            @Size(min = 1, max = 16, message = "el armado debe tener entre 1 y 16 componentes")
            List<@NotNull(message = "no se admiten productos vacíos") Long> productoIds,
            @NotNull(message = "elige el uso previsto") UsoPrevisto uso) {
    }

    /** Uso a considerar si la configuración guardada no tiene uno. */
    public record SolicitudGuardada(UsoPrevisto uso) {
    }

    private final OptimizacionService servicio;

    public OptimizacionController(OptimizacionService servicio) {
        this.servicio = servicio;
    }

    /** Análisis en vivo del armado (sin sesión; no se guarda). */
    @PostMapping("/analizar")
    public OptimizacionService.Analisis analizar(@Valid @RequestBody Solicitud s) {
        return servicio.analizar(s.productoIds(), s.uso());
    }

    /** Analiza una configuración guardada propia y guarda el resultado. */
    @PostMapping("/configuraciones/{id}")
    public OptimizacionService.Analisis analizarGuardada(@RequestHeader(Identidad.USUARIO_ID) Long yo, @PathVariable Long id,
                                                        @RequestBody(required = false) SolicitudGuardada s) {
        return servicio.analizarConfiguracion(yo, id, s == null ? null : s.uso());
    }

    /** Último análisis guardado de una configuración propia. */
    @GetMapping("/configuraciones/{id}")
    public OptimizacionService.Guardado guardado(@RequestHeader(Identidad.USUARIO_ID) Long yo, @PathVariable Long id) {
        return servicio.guardado(yo, id);
    }
}
