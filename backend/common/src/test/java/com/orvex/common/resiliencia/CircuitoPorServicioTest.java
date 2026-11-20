package com.orvex.common.resiliencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class CircuitoPorServicioTest {

    @Test
    void diezFallasAbrenElCircuitoYLaLlamadaYaNoSeIntenta() {
        CircuitBreakerRegistry registro = CircuitoPorServicio.registroPorDefecto();
        RestClient.Builder b = RestClient.builder().baseUrl("http://catalogo:8102").requestInterceptor(new CircuitoPorServicio(registro));
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(b).build();
        servidor.expect(ExpectedCount.times(10), requestTo("http://catalogo:8102/x")).andRespond(withServerError());
        RestClient http = b.build();
        for (int i = 0; i < 10; i++) {
            assertThrows(HttpServerErrorException.class, () -> http.get().uri("/x").retrieve().toBodilessEntity());
        }
        assertEquals(CircuitBreaker.State.OPEN, registro.circuitBreaker("catalogo").getState());
        // Abierto: falla en el acto sin llamar (el servidor simulado no espera más pedidos).
        assertThrows(ResourceAccessException.class, () -> http.get().uri("/x").retrieve().toBodilessEntity());
        servidor.verify();
    }

    @Test
    void unCuatrocientosNoCuentaComoFalla() {
        CircuitBreakerRegistry registro = CircuitoPorServicio.registroPorDefecto();
        RestClient.Builder b = RestClient.builder().baseUrl("http://inventario:8105").requestInterceptor(new CircuitoPorServicio(registro));
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(b).build();
        servidor.expect(ExpectedCount.times(12), requestTo("http://inventario:8105/r")).andRespond(withStatus(HttpStatus.CONFLICT));
        servidor.expect(requestTo("http://inventario:8105/ok")).andRespond(withSuccess());
        RestClient http = b.build();
        for (int i = 0; i < 12; i++) {
            assertThrows(HttpClientErrorException.class, () -> http.post().uri("/r").retrieve().toBodilessEntity());
        }
        assertEquals(CircuitBreaker.State.CLOSED, registro.circuitBreaker("inventario").getState());
        http.get().uri("/ok").retrieve().toBodilessEntity();
        servidor.verify();
    }
}
