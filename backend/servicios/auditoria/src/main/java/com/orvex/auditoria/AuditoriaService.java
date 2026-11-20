package com.orvex.auditoria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orvex.eventos.Evento;
import com.orvex.eventos.PublicadorEventos;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Auditoría (RF-26, HU-21) */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    public static final String COLA = "orvex.auditoria";

    public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
        boolean existsByEventoId(String eventoId);

        @Query("""
                SELECT a FROM Auditoria a
                WHERE (:accion IS NULL OR a.accion = :accion) AND (:entidad IS NULL OR a.entidad = :entidad)
                  AND (:usuario IS NULL OR a.usuarioId = :usuario) AND (:desde IS NULL OR a.fecha >= :desde)
                  AND (:hasta IS NULL OR a.fecha < :hasta)
                ORDER BY a.fecha DESC, a.id DESC
                """)
        Page<Auditoria> buscar(@Param("accion") String accion, @Param("entidad") String entidad, @Param("usuario") Long usuario,
                               @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                               org.springframework.data.domain.Pageable p);

        @Query("SELECT DISTINCT a.accion FROM Auditoria a ORDER BY a.accion")
        List<String> acciones();
    }

    /** Cola durable que recibe todo (#); lo que falla 3 veces va a la cola de fallidos para revisarlo. */
    @Configuration
    static class Cola {
        @Bean
        Queue colaAuditoria() {
            return QueueBuilder.durable(COLA).deadLetterExchange("").deadLetterRoutingKey(COLA + ".fallidos").build();
        }

        @Bean
        Queue colaAuditoriaFallidos() {
            return QueueBuilder.durable(COLA + ".fallidos").build();
        }

        @Bean
        Binding todoAAuditoria(Queue colaAuditoria, TopicExchange orvexEventos) {
            return BindingBuilder.bind(colaAuditoria).to(orvexEventos).with("#");
        }
    }

    public record Registro(Long id, Long usuarioId, String accion, String entidad, String detalle, LocalDateTime fecha) {
    }

    public record Pagina(List<Registro> elementos, int pagina, int totalPaginas, long total) {
    }

    private final AuditoriaRepository repo;
    private final ObjectMapper json;

    public AuditoriaService(AuditoriaRepository repo, ObjectMapper json) {
        this.repo = repo;
        this.json = json;
    }

    @RabbitListener(queues = COLA)
    public void recibir(Evento e) {
        registrar(e);
    }

    /** Idempotente por id de evento: RabbitMQ puede entregar dos veces el mismo mensaje. */
    @Transactional
    public boolean registrar(Evento e) {
        if (e == null || e.tipo() == null) {
            return false;
        }
        if (e.id() != null && repo.existsByEventoId(e.id())) {
            return false;
        }
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("servicio", e.servicio());
        if (e.entidadId() != null) {
            detalle.put("id", e.entidadId());
        }
        detalle.putAll(e.datos() == null ? Map.of() : e.datos());
        String texto;
        try {
            texto = json.writeValueAsString(detalle);
        } catch (JsonProcessingException ex) {
            texto = detalle.toString();
        }
        LocalDateTime fecha = e.fecha() == null ? LocalDateTime.now(ZoneOffset.UTC) : LocalDateTime.ofInstant(e.fecha(), ZoneOffset.UTC);
        try {
            repo.saveAndFlush(new Auditoria(e.usuarioId(), recortar(e.tipo(), 60), recortar(e.entidad() == null ? "-" : e.entidad(), 40),
                    texto, fecha, e.id()));
            return true;
        } catch (DataIntegrityViolationException dup) {
            log.debug("Evento {} ya auditado", e.id());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Pagina buscar(String accion, String entidad, Long usuario, LocalDateTime desde, LocalDateTime hasta, int pagina) {
        var p = repo.buscar(vacioANull(accion), vacioANull(entidad), usuario, desde, hasta, PageRequest.of(Math.max(0, pagina), 30));
        return new Pagina(p.getContent().stream().map(a -> new Registro(a.getId(), a.getUsuarioId(), a.getAccion(), a.getEntidad(),
                a.getDetalle(), a.getFecha())).toList(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<String> acciones() {
        return repo.acciones();
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String recortar(String s, int n) {
        return s.length() > n ? s.substring(0, n) : s;
    }

    /** Solo para que el exchange exista aunque este servicio arranque antes que los que publican. */
    static final String EXCHANGE = PublicadorEventos.EXCHANGE;
}
