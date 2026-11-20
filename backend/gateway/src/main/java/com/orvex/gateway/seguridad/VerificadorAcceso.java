package com.orvex.gateway.seguridad;

import com.orvex.gateway.ServiciosProperties;
import com.orvex.seguridad.Rol;
import com.orvex.seguridad.TokenJwt;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class VerificadorAcceso {

    private static final Logger log = LoggerFactory.getLogger(VerificadorAcceso.class);
    static final Duration TIMEOUT = Duration.ofMillis(1500);

    public record Acceso(long usuarioId, Rol rol, boolean activo) {
    }

    record AccesoRemoto(String rol, String estado) {
    }

    private record EnCache(Acceso acceso, Instant hasta) {
    }

    private final Map<Long, EnCache> cache = new ConcurrentHashMap<>();
    private final WebClient http;
    private final Duration duracionCache;

    public VerificadorAcceso(WebClient.Builder builder, ServiciosProperties props,
                             @Value("${orvex.acceso.cache:30s}") Duration duracionCache) {
        this.http = builder.baseUrl(props.servicios().get("usuarios")).build();
        this.duracionCache = duracionCache;
    }

    /** Vacío si el usuario ya no existe. */
    public Mono<Optional<Acceso>> verificar(TokenJwt.Sesion sesion) {
        EnCache c = cache.get(sesion.usuarioId());
        if (c != null && Instant.now().isBefore(c.hasta())) {
            return Mono.just(Optional.of(c.acceso()));
        }
        return http.get().uri("/internal/usuarios/{id}/acceso", sesion.usuarioId())
                .retrieve()
                .bodyToMono(AccesoRemoto.class)
                .timeout(TIMEOUT)
                .map(a -> {
                    var acceso = new Acceso(sesion.usuarioId(), Rol.valueOf(a.rol()), "ACTIVO".equals(a.estado()));
                    cache.put(sesion.usuarioId(), new EnCache(acceso, Instant.now().plus(duracionCache)));
                    return Optional.of(acceso);
                })
                .onErrorResume(WebClientResponseException.class, e -> e.getStatusCode() == HttpStatus.NOT_FOUND
                        ? Mono.just(Optional.empty())
                        : degradado(sesion, e))
                .onErrorResume(e -> degradado(sesion, e));
    }

    private Mono<Optional<Acceso>> degradado(TokenJwt.Sesion sesion, Throwable e) {
        log.warn("Servicio de usuarios no disponible; se usa el rol del token (usuario {}): {}",
                sesion.usuarioId(), e.toString());
        return Mono.just(Optional.of(new Acceso(sesion.usuarioId(), sesion.rol(), true)));
    }

    /** Olvida lo guardado de un usuario (por ejemplo al cerrar sesión). */
    public void olvidar(long usuarioId) {
        cache.remove(usuarioId);
    }
}
