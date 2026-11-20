package com.orvex.compatibilidad.servicio;

import com.orvex.eventos.PublicadorEventos;
import com.orvex.common.web.ApiException;
import com.orvex.compat.Categoria;
import com.orvex.compat.MotorCompatibilidad;
import com.orvex.compat.Regla;
import com.orvex.compat.reglas.ReglasPorDefecto;
import com.orvex.compatibilidad.modelo.ReglaCompatibilidad;
import com.orvex.compatibilidad.modelo.Repositorios.ConfiguracionRepository;
import com.orvex.compatibilidad.modelo.Repositorios.ReglaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reglas de compatibilidad guardadas en la base de datos (RNF-06). */
@Service
public class ReglasService {

    private static final Logger log = LoggerFactory.getLogger(ReglasService.class);
    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");
    static final Duration CACHE = Duration.ofSeconds(30);

    public record ReglaDto(Long id, Categoria categoriaA, Categoria categoriaB, String nombre, String condicion) {
        static ReglaDto de(ReglaCompatibilidad r) {
            return new ReglaDto(r.getId(), r.getCategoriaA(), r.getCategoriaB(), r.getNombre(), r.getCondicion());
        }
    }

    public record Entrada(Categoria categoriaA, Categoria categoriaB, String nombre, String condicion) {
    }

    private record EnCache(MotorCompatibilidad motor, Instant hasta) {
    }

    private final ReglaRepository reglas;
    private final ConfiguracionRepository configuraciones;
    private volatile EnCache cache;

    private final PublicadorEventos eventos;

    public ReglasService(ReglaRepository reglas, ConfiguracionRepository configuraciones, PublicadorEventos eventos) {
        this.eventos = eventos;
        this.reglas = reglas;
        this.configuraciones = configuraciones;
    }

    @Transactional(readOnly = true)
    public MotorCompatibilidad motor() {
        EnCache c = cache;
        if (c != null && Instant.now().isBefore(c.hasta())) {
            return c.motor();
        }
        List<Regla> lista = new ArrayList<>();
        for (ReglaCompatibilidad r : reglas.findAllByOrderByIdAsc()) {
            try {
                lista.add(ReglasPorDefecto.crear(r.getId(), r.getNombre(), r.getCategoriaA(), r.getCategoriaB(), r.getCondicion()));
            } catch (IllegalArgumentException e) {
                // Solo pasa si alguien editó la tabla a mano: la regla rota se omite y se avisa en el log.
                log.error("Regla {} ('{}') ignorada: {}", r.getId(), r.getNombre(), e.getMessage());
            }
        }
        MotorCompatibilidad m = new MotorCompatibilidad(lista);
        cache = new EnCache(m, Instant.now().plus(CACHE));
        return m;
    }

    @Transactional(readOnly = true)
    public List<ReglaDto> listar() {
        return reglas.findAllByOrderByIdAsc().stream().map(ReglaDto::de).toList();
    }

    @Transactional
    public ReglaDto crear(Long adminId, Entrada e) {
        validar(e);
        var r = reglas.save(new ReglaCompatibilidad(e.categoriaA(), e.categoriaB(), e.nombre().trim(), e.condicion().trim()));
        auditoria.info("accion=regla_creada usuario_responsable={} regla={} condicion=\"{}\"", adminId, r.getId(), r.getCondicion());
        eventos.publicar("regla.creada", adminId, "regla_compatibilidad", r.getId(), Map.of("nombre", r.getNombre(), "condicion", r.getCondicion()));
        cache = null;
        return ReglaDto.de(r);
    }

    @Transactional
    public ReglaDto actualizar(Long adminId, Long id, Entrada e) {
        validar(e);
        var r = reglas.findById(id).orElseThrow(() -> ApiException.noEncontrado("La regla no existe."));
        String anterior = r.getCondicion();
        r.actualizar(e.categoriaA(), e.categoriaB(), e.nombre().trim(), e.condicion().trim());
        auditoria.info("accion=regla_modificada usuario_responsable={} regla={} anterior=\"{}\" nueva=\"{}\"",
                adminId, id, anterior, r.getCondicion());
        eventos.publicar("regla.modificada", adminId, "regla_compatibilidad", id,
                Map.of("nombre", r.getNombre(), "anterior", anterior, "nueva", r.getCondicion()));
        cache = null;
        return ReglaDto.de(r);
    }

    @Transactional
    public void eliminar(Long adminId, Long id) {
        var r = reglas.findById(id).orElseThrow(() -> ApiException.noEncontrado("La regla no existe."));
        if (configuraciones.resultadosDeRegla(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta regla ya se usó para verificar configuraciones guardadas: "
                    + "en lugar de eliminarla, edita su condición.");
        }
        reglas.delete(r);
        auditoria.info("accion=regla_eliminada usuario_responsable={} regla={} nombre=\"{}\"", adminId, id, r.getNombre());
        eventos.publicar("regla.eliminada", adminId, "regla_compatibilidad", id, Map.of("nombre", r.getNombre()));
        cache = null;
    }

    /** Se interpreta la condición antes de guardarla: una regla mal escrita nunca llega a la tabla. */
    static void validar(Entrada e) {
        if (e.categoriaA() == null || e.categoriaB() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Elige las dos categorías.", Map.of("categoriaA", "elige las categorías"));
        }
        if (e.nombre() == null || e.nombre().isBlank() || e.nombre().trim().length() > 80) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "nombre: entre 1 y 80 caracteres", Map.of("nombre", "entre 1 y 80 caracteres"));
        }
        if (e.condicion() == null || e.condicion().trim().length() > 255) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "condicion: hasta 255 caracteres", Map.of("condicion", "hasta 255 caracteres"));
        }
        try {
            ReglasPorDefecto.crear(null, e.nombre(), e.categoriaA(), e.categoriaB(), e.condicion());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of("condicion", ex.getMessage()));
        }
    }
}
