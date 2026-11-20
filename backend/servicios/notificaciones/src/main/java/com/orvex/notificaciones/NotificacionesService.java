package com.orvex.notificaciones;

import com.orvex.eventos.Evento;
import com.orvex.notificaciones.Notificacion.Estado;
import com.orvex.notificaciones.Notificacion.Tipo;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Notificaciones (RF-20, HU-14, HU-18): convierte eventos de la cola en avisos por correo. */
@Service
public class NotificacionesService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesService.class);
    public static final String COLA = "orvex.notificaciones";

    public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
        List<Notificacion> findByEstadoOrderByIdAsc(Estado estado, org.springframework.data.domain.Pageable p);

        List<Notificacion> findByUsuarioIdOrderByIdDesc(Long usuarioId, org.springframework.data.domain.Pageable p);

        long countByEstado(Estado estado);

        @Query("SELECT COUNT(n) > 0 FROM Notificacion n WHERE n.eventoId = :evento AND n.usuarioId = :usuario")
        boolean existe(@org.springframework.data.repository.query.Param("evento") String evento,
                       @org.springframework.data.repository.query.Param("usuario") Long usuario);
    }

    @Configuration
    static class Cola {
        @Bean
        Declarables colaNotificaciones(TopicExchange orvexEventos) {
            Queue cola = QueueBuilder.durable(COLA).deadLetterExchange("").deadLetterRoutingKey(COLA + ".fallidos").build();
            List<org.springframework.amqp.core.Declarable> d = new ArrayList<>(List.of(cola, QueueBuilder.durable(COLA + ".fallidos").build()));
            for (String clave : List.of("pago.confirmado", "pedido.estado_cambiado", "pedido.cancelado", "stock.bajo")) {
                Binding b = BindingBuilder.bind(cola).to(orvexEventos).with(clave);
                d.add(b);
            }
            return new Declarables(d);
        }
    }

    public record Vista(Long id, Tipo tipo, String mensaje, LocalDateTime fecha, Estado estado) {
    }

    public record Resumen(long pendientes, long enviadas, long fallidas, String circuito) {
    }

    private final NotificacionRepository repo;
    private final UsuariosCliente usuarios;
    private final JavaMailSender correo;
    private final TransactionTemplate tx;
    private final Clock reloj;
    private final String remitente;
    private final CircuitBreaker circuito;
    private final Retry reintentos;

    public NotificacionesService(NotificacionRepository repo, UsuariosCliente usuarios, JavaMailSender correo, TransactionTemplate tx,
                                 Clock reloj, @Value("${orvex.correo.remitente:Orvex <no-responder@orvex.local>}") String remitente) {
        this.repo = repo;
        this.usuarios = usuarios;
        this.correo = correo;
        this.tx = tx;
        this.reloj = reloj;
        this.remitente = remitente;
        // 5 fallas en las últimas 10 llamadas abren el circuito 30 s; luego deja pasar 2 de prueba.
        this.circuito = CircuitBreaker.of("smtp", CircuitBreakerConfig.custom().slidingWindowSize(10).minimumNumberOfCalls(5)
                .failureRateThreshold(50).waitDurationInOpenState(Duration.ofSeconds(30)).permittedNumberOfCallsInHalfOpenState(2)
                .build());
        this.reintentos = Retry.of("smtp", RetryConfig.custom().maxAttempts(3).waitDuration(Duration.ofMillis(500))
                .retryExceptions(MailException.class).build());
        circuito.getEventPublisher().onStateTransition(e -> log.warn("ALERTA_CORREO circuito del servidor de correo: {}",
                e.getStateTransition()));
    }

    // eventos → avisos

    @RabbitListener(queues = COLA)
    public void recibir(Evento e) {
        procesar(e);
    }

    /** Cuántos avisos generó el evento (0 si no corresponde o ya se había procesado). */
    @Transactional
    public int procesar(Evento e) {
        if (e == null || e.tipo() == null) {
            return 0;
        }
        List<Notificacion> avisos = switch (e.tipo()) {
            case "pago.confirmado" -> cliente(e, Tipo.PAGO, "Recibimos tu pago de " + bs(e.texto("total")) + " del pedido #"
                    + e.entidadId() + ". Tu recibo interno es " + e.texto("recibo") + ".");
            case "pedido.estado_cambiado" -> {
                String estado = e.texto("estado");
                String texto = "PREPARADO".equals(estado) ? "está preparado: pronto lo entregamos."
                        : "ENTREGADO".equals(estado) ? "fue entregado. ¡Gracias por comprar en Orvex!" : null;
                yield texto == null ? List.of() : cliente(e, Tipo.ESTADO_PEDIDO, "Tu pedido #" + e.entidadId() + " " + texto);
            }
            case "pedido.cancelado" -> cliente(e, Tipo.ESTADO_PEDIDO, "Tu pedido #" + e.entidadId() + " se canceló ("
                    + Optional.ofNullable(e.texto("motivo")).orElse("sin pago") + "). Los productos volvieron a la venta.");
            case "stock.bajo" -> personal(e);
            default -> List.of();
        };
        int n = 0;
        for (Notificacion a : avisos) {
            if (e.id() != null && repo.existe(e.id(), a.getUsuarioId())) {
                continue; // entrega repetida de la cola
            }
            try {
                repo.saveAndFlush(a);
                n++;
            } catch (DataIntegrityViolationException dup) {
                log.debug("Aviso duplicado del evento {}", e.id());
            }
        }
        return n;
    }

    private List<Notificacion> cliente(Evento e, Tipo tipo, String mensaje) {
        Long cliente = e.numero("clienteId");
        if (cliente == null || !"LINEA".equals(e.texto("canal"))) {
            return List.of(); // venta en tienda: el cliente está presente y se lleva su recibo
        }
        return List.of(new Notificacion(cliente, tipo, mensaje, LocalDateTime.now(reloj), e.id()));
    }

    /** Stock bajo: a cada almacenero y administrador activo. */
    private List<Notificacion> personal(Evento e) {
        String producto = Optional.ofNullable(e.texto("nombre")).orElse("el producto #" + e.entidadId());
        String mensaje = "Stock bajo: " + producto + " tiene " + e.texto("disponible") + " unidades (mínimo " + e.texto("minimo")
                + "). Revisa la política de reposición.";
        return usuarios.personal(List.of("ALMACENERO", "ADMINISTRADOR")).stream()
                .map(c -> new Notificacion(c.id(), Tipo.STOCK_BAJO, mensaje, LocalDateTime.now(reloj), e.id())).toList();
    }

    // envío (tarea programada)

    /** Envía los pendientes en orden */
    @Scheduled(fixedDelayString = "${orvex.notificaciones.envio-ms:5000}", initialDelayString = "${orvex.notificaciones.envio-ms:5000}")
    public int enviarPendientes() {
        int enviadas = 0;
        List<Notificacion> lote = repo.findByEstadoOrderByIdAsc(Estado.PENDIENTE, PageRequest.of(0, 50));
        for (Notificacion n : lote) {
            var contacto = usuarios.contacto(n.getUsuarioId());
            if (contacto.isEmpty() || contacto.get().correo() == null) {
                if (contacto.isEmpty() && !usuarios.disponible()) {
                    return enviadas; // usuarios no responde: se reintenta en la próxima vuelta
                }
                marcar(n.getId(), false);
                continue;
            }
            try {
                circuito.executeRunnable(Retry.decorateRunnable(reintentos, () -> enviarCorreo(contacto.get().correo(), n)));
                marcar(n.getId(), true);
                enviadas++;
            } catch (CallNotPermittedException abierto) {
                log.info("Circuito del correo abierto: {} avisos esperan", repo.countByEstado(Estado.PENDIENTE));
                return enviadas;
            } catch (MailException fallo) {
                log.warn("ALERTA_CORREO no se pudo enviar el aviso {} tras 3 intentos: {}", n.getId(), fallo.getMessage());
                return enviadas; // queda pendiente: sale cuando el servidor de correo vuelva
            }
        }
        return enviadas;
    }

    private void enviarCorreo(String destino, Notificacion n) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(remitente);
        m.setTo(destino);
        m.setSubject(asunto(n.getTipo()));
        m.setText(n.getMensaje() + "\n\n— Orvex, componentes de PC.\nEste es un aviso automático; no respondas a este correo.");
        correo.send(m);
    }

    private void marcar(Long id, boolean ok) {
        tx.executeWithoutResult(s -> repo.findById(id).ifPresent(n -> {
            if (ok) {
                n.enviada(LocalDateTime.now(reloj));
            } else {
                n.fallida();
            }
        }));
    }

    static String asunto(Tipo t) {
        return switch (t) {
            case PAGO -> "Orvex: pago confirmado";
            case ESTADO_PEDIDO -> "Orvex: novedades de tu pedido";
            case STOCK_BAJO -> "Orvex: alerta de stock bajo";
            case BIENVENIDA -> "Bienvenido a Orvex";
            case RECUPERACION -> "Orvex: recupera tu contraseña";
        };
    }

    private static String bs(String monto) {
        try {
            return "Bs " + new BigDecimal(monto).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        } catch (RuntimeException e) {
            return "Bs " + monto;
        }
    }

    // consultas

    @Transactional(readOnly = true)
    public List<Vista> mias(Long usuarioId) {
        return repo.findByUsuarioIdOrderByIdDesc(usuarioId, PageRequest.of(0, 20)).stream()
                .map(n -> new Vista(n.getId(), n.getTipo(), n.getMensaje(), n.getFecha(), n.getEstado())).toList();
    }

    @Transactional(readOnly = true)
    public Resumen resumen() {
        return new Resumen(repo.countByEstado(Estado.PENDIENTE), repo.countByEstado(Estado.ENVIADO), repo.countByEstado(Estado.FALLIDO),
                circuito.getState().name());
    }

    CircuitBreaker circuito() {
        return circuito;
    }

    static Map<String, Object> vacio() {
        return Map.of();
    }
}
