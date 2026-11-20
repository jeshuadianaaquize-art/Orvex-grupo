package com.orvex.catalogo.servicio;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Pregunta al servicio de Inventario las unidades disponibles de cada producto. */
@Component
public class InventarioCliente {

    private static final Logger log = LoggerFactory.getLogger(InventarioCliente.class);
    static final int LOTE = 200;

    public record Existencias(Long productoId, int vendibles, int reservadas, int minimo) {
    }

    private final RestClient http;

    public InventarioCliente(RestClient.Builder builder, @Value("${orvex.servicios.inventario-url}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    public Optional<Map<Long, Existencias>> existencias(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Optional.of(Map.of());
        }
        try {
            List<Long> lista = ids.stream().distinct().toList();
            Map<Long, Existencias> r = new java.util.HashMap<>();
            for (int i = 0; i < lista.size(); i += LOTE) {
                String parte = lista.subList(i, Math.min(lista.size(), i + LOTE)).stream()
                        .map(String::valueOf).collect(Collectors.joining(","));
                List<Existencias> e = http.get().uri(u -> u.path("/internal/stock").queryParam("ids", parte).build())
                        .retrieve().body(new ParameterizedTypeReference<List<Existencias>>() { });
                if (e != null) {
                    r.putAll(e.stream().collect(Collectors.toMap(Existencias::productoId, Function.identity())));
                }
            }
            return Optional.of(r);
        } catch (RestClientException e) {
            log.warn("Inventario no disponible; la disponibilidad queda por confirmar: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
