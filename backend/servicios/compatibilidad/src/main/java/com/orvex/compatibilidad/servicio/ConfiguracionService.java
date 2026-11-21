package com.orvex.compatibilidad.servicio;

import com.orvex.common.web.ApiException;
import com.orvex.compat.Severidad;
import com.orvex.compatibilidad.modelo.ConfiguracionPc;
import com.orvex.compatibilidad.modelo.ConfiguracionPc.Resultado;
import com.orvex.compatibilidad.modelo.Modelo.UsoPrevisto;
import com.orvex.compatibilidad.modelo.Repositorios.ConfiguracionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Configuraciones de PC guardadas por el cliente */
@Service
public class ConfiguracionService {

    static final int MAXIMO_POR_USUARIO = 20;

    public record Resumen(Long id, String nombre, UsoPrevisto uso, BigDecimal precioTotal, Integer consumoEstimadoW,
                          int componentes, Severidad estado, LocalDateTime fechaCreacion) {
    }

    public record Detalle(Resumen configuracion, List<Long> productoIds, VerificacionService.Resultado verificacionActual) {
    }

    private final ConfiguracionRepository configuraciones;
    private final VerificacionService verificacion;
    private final Clock reloj;

    public ConfiguracionService(ConfiguracionRepository configuraciones, VerificacionService verificacion, Clock reloj) {
        this.configuraciones = configuraciones;
        this.verificacion = verificacion;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<Resumen> listar(Long usuarioId) {
        return configuraciones.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream().map(ConfiguracionService::resumen).toList();
    }

    @Transactional
    public Detalle guardar(Long usuarioId, String nombre, UsoPrevisto uso, List<Long> productoIds) {
        if (configuraciones.countByUsuarioId(usuarioId) >= MAXIMO_POR_USUARIO) {
            throw new ApiException(HttpStatus.CONFLICT, "Puedes guardar hasta " + MAXIMO_POR_USUARIO
                    + " configuraciones. Elimina alguna para guardar otra.");
        }
        var c = new ConfiguracionPc(usuarioId, LocalDateTime.now(reloj));
        var v = aplicar(c, nombre, uso, productoIds);
        c = configuraciones.save(c);
        return new Detalle(resumen(c), ids(c), v);
    }

    @Transactional
    public Detalle actualizar(Long usuarioId, Long id, String nombre, UsoPrevisto uso, List<Long> productoIds) {
        var c = propia(usuarioId, id);
        var v = aplicar(c, nombre, uso, productoIds);
        return new Detalle(resumen(c), ids(c), v);
    }

    /** Para otros servicios */
    public record Interna(Long id, String nombre, UsoPrevisto uso, List<Long> productoIds) {
    }

    @Transactional(readOnly = true)
    public Interna interna(Long usuarioId, Long id) {
        var c = propia(usuarioId, id);
        return new Interna(c.getId(), c.getNombre(), c.getUso(), ids(c));
    }

    /** Abre una configuración y la vuelve a verificar con el catálogo y las reglas de hoy. */
    @Transactional(readOnly = true)
    public Detalle abrir(Long usuarioId, Long id) {
        var c = propia(usuarioId, id);
        VerificacionService.Resultado actual = null;
        try {
            actual = verificacion.verificar(ids(c));
        } catch (IllegalArgumentException e) {
            // Algún producto ya no está publicado: se devuelve igual para que el cliente lo reemplace.
        }
        return new Detalle(resumen(c), ids(c), actual);
    }

    @Transactional
    public void eliminar(Long usuarioId, Long id) {
        configuraciones.delete(propia(usuarioId, id));
    }

    private VerificacionService.Resultado aplicar(ConfiguracionPc c, String nombre, UsoPrevisto uso, List<Long> productoIds) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.isEmpty() || n.length() > 80) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "nombre: entre 1 y 80 caracteres", Map.of("nombre", "entre 1 y 80 caracteres"));
        }
        var v = verificacion.verificar(productoIds);
        List<Resultado.Datos> resultados = v.reporte().resultados().stream()
                .filter(r -> r.reglaId() != null) // "Cobertura" no es una regla de la tabla
                .map(r -> new Resultado.Datos(r.reglaId(), r.severidad(), r.mensaje()))
                .toList();
        int consumo = v.reporte().consumoEstimadoW();
        c.actualizar(n, uso, v.componentes().stream().map(p -> p.id()).toList(), consumo > 0 ? consumo : null,
                v.precioTotal(), resultados);
        return v;
    }

    /** Cada usuario solo ve y modifica sus configuraciones (si el id es de otro, responde 404, OWASP A01). */
    private ConfiguracionPc propia(Long usuarioId, Long id) {
        return configuraciones.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> ApiException.noEncontrado("La configuración no existe."));
    }

    private static List<Long> ids(ConfiguracionPc c) {
        return c.getItems().stream().map(ConfiguracionPc.Item::getProductoId).toList();
    }

    private static Resumen resumen(ConfiguracionPc c) {
        return new Resumen(c.getId(), c.getNombre(), c.getUso(), c.getPrecioTotal(), c.getConsumoEstimadoW(),
                c.getItems().size(), c.estado(), c.getFechaCreacion());
    }
}
