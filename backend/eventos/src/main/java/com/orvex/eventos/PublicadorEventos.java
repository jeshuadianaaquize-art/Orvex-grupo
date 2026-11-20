package com.orvex.eventos;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Publica eventos de negocio en el exchange {@value #EXCHANGE} */
public class PublicadorEventos {

    public static final String EXCHANGE = "orvex.eventos";
    static final int MAXIMO_PENDIENTES = 2000;
    static final int REINTENTO_S = 10;

    private static final Logger log = LoggerFactory.getLogger("EVENTO");

    private final RabbitTemplate rabbit;
    private final String servicio;
    private final boolean habilitado;
    private final ConcurrentLinkedDeque<Evento> pendientes = new ConcurrentLinkedDeque<>();
    private final AtomicInteger cantidadPendientes = new AtomicInteger();
    private final ScheduledExecutorService hilo = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "orvex-eventos");
        t.setDaemon(true);
        return t;
    });

    public PublicadorEventos(RabbitTemplate rabbit, String servicio, boolean habilitado) {
        this.rabbit = rabbit;
        this.servicio = servicio;
        this.habilitado = habilitado;
        if (habilitado) {
            hilo.scheduleWithFixedDelay(this::reintentar, REINTENTO_S, REINTENTO_S, TimeUnit.SECONDS);
        }
    }

    public void publicar(String tipo, Long usuarioId, String entidad, Object entidadId, Map<String, Object> datos) {
        Evento e = Evento.nuevo(tipo, servicio, usuarioId, entidad, entidadId, datos);
        if (!habilitado) {
            log.info("{} usuario={} {}={} {}", tipo, usuarioId, entidad, entidadId, e.datos());
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    encolar(e);
                }
            });
        } else {
            encolar(e);
        }
    }

    /** Eventos que esperan a que RabbitMQ vuelva (para el panel de estado y las pruebas). */
    public int pendientes() {
        return cantidadPendientes.get();
    }

    private void encolar(Evento e) {
        hilo.execute(() -> {
            if (!pendientes.isEmpty()) {
                guardar(e); // respeta el orden: primero salen los que ya esperaban
                reintentar();
                return;
            }
            if (!enviar(e)) {
                guardar(e);
            }
        });
    }

    private boolean enviar(Evento e) {
        try {
            rabbit.convertAndSend(EXCHANGE, e.tipo(), e);
            log.debug("{} enviado id={}", e.tipo(), e.id());
            return true;
        } catch (AmqpException ex) {
            log.warn("{} no se pudo enviar (RabbitMQ no responde: {}); queda pendiente id={}", e.tipo(), ex.getMessage(), e.id());
            return false;
        }
    }

    private void guardar(Evento e) {
        if (cantidadPendientes.get() >= MAXIMO_PENDIENTES) {
            Evento viejo = pendientes.pollFirst();
            if (viejo != null) {
                cantidadPendientes.decrementAndGet();
                log.error("{} descartado por cola llena id={} {}", viejo.tipo(), viejo.id(), viejo.datos());
            }
        }
        pendientes.addLast(e);
        cantidadPendientes.incrementAndGet();
    }

    void reintentar() {
        Evento e;
        while ((e = pendientes.peekFirst()) != null) {
            if (!enviar(e)) {
                return;
            }
            pendientes.pollFirst();
            cantidadPendientes.decrementAndGet();
        }
    }
}
