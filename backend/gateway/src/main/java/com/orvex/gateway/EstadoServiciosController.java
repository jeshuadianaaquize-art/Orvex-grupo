package com.orvex.gateway;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** GET /api/estado: consulta en paralelo el health check de cada microservicio (HU-25, RF-23). */
@RestController
public class EstadoServiciosController {

    static final Duration TIMEOUT = Duration.ofSeconds(2);

    public record EstadoServicio(String servicio, String estado, long latenciaMs) {
    }

    public record EstadoGeneral(String estado, Instant fecha, List<EstadoServicio> servicios) {
    }

    private final ServiciosProperties props;
    private final WebClient http;

    public EstadoServiciosController(ServiciosProperties props, WebClient.Builder builder) {
        this.props = props;
        this.http = builder.build();
    }

    @GetMapping("/api/estado")
    public Mono<EstadoGeneral> estado() {
        return Flux.fromIterable(props.servicios().entrySet())
                .flatMap(this::consultar)
                .sort(Comparator.comparing(EstadoServicio::servicio))
                .collectList()
                .map(lista -> new EstadoGeneral(
                        lista.stream().allMatch(s -> "UP".equals(s.estado())) ? "UP" : "DEGRADADO",
                        Instant.now(), lista));
    }

    private Mono<EstadoServicio> consultar(Map.Entry<String, String> servicio) {
        long inicio = System.nanoTime();
        return http.get().uri(servicio.getValue() + "/actuator/health")
                .exchangeToMono(r -> r.bodyToMono(Map.class).defaultIfEmpty(Map.of()))
                .map(cuerpo -> String.valueOf(cuerpo.getOrDefault("status", "DOWN")))
                .timeout(TIMEOUT)
                .onErrorReturn("DOWN")
                .map(estado -> new EstadoServicio(servicio.getKey(), estado, (System.nanoTime() - inicio) / 1_000_000));
    }
}
