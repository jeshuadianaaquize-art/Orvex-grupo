package com.orvex.common.resiliencia;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;

/** Circuit Breaker por servicio destino */
public class CircuitoPorServicio implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CircuitoPorServicio.class);

    private final CircuitBreakerRegistry registro;

    public CircuitoPorServicio(CircuitBreakerRegistry registro) {
        this.registro = registro;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest pedido, byte[] cuerpo, ClientHttpRequestExecution ejecucion) throws IOException {
        String destino = pedido.getURI().getHost() == null ? "desconocido" : pedido.getURI().getHost();
        CircuitBreaker cb = registro.circuitBreaker(destino);
        if (!cb.tryAcquirePermission()) {
            throw new ResourceAccessException("Circuito abierto hacia " + destino + ": la llamada no se intenta.");
        }
        long inicio = System.nanoTime();
        try {
            ClientHttpResponse r = ejecucion.execute(pedido, cuerpo);
            long dur = System.nanoTime() - inicio;
            if (r.getStatusCode().is5xxServerError()) {
                cb.onError(dur, TimeUnit.NANOSECONDS, new IOException("HTTP " + r.getStatusCode().value()));
            } else {
                cb.onResult(dur, TimeUnit.NANOSECONDS, r);
            }
            return r;
        } catch (IOException | RuntimeException e) {
            cb.onError(System.nanoTime() - inicio, TimeUnit.NANOSECONDS, e);
            throw e;
        }
    }

    /** Configuración propuesta (D51): 10 llamadas de muestra, 50 % de fallas abre 15 s, luego 3 de prueba. */
    public static CircuitBreakerRegistry registroPorDefecto() {
        var cfg = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .slowCallDurationThreshold(java.time.Duration.ofSeconds(4))
                .slowCallRateThreshold(80)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        CircuitBreakerRegistry r = CircuitBreakerRegistry.of(cfg);
        r.getEventPublisher().onEntryAdded(e -> e.getAddedEntry().getEventPublisher().onStateTransition(t ->
                log.warn("ALERTA_CIRCUITO {}: {}", t.getCircuitBreakerName(), t.getStateTransition())));
        return r;
    }
}
