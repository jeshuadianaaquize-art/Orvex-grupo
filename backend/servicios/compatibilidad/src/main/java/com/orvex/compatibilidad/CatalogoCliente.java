package com.orvex.compatibilidad;

import com.orvex.common.web.ServicioNoDisponibleException;
import com.orvex.compat.Categoria;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Consulta al servicio de Catálogo los productos del armado */
@Component
public class CatalogoCliente {

    /** Contrato de la respuesta de GET /api/productos del servicio de Catálogo. */
    public record ProductoCatalogo(Long id, String sku, String nombre, String marca, BigDecimal precio, String fotoUrl,
                                   Categoria categoria, Map<String, String> specs, Integer stock, Boolean enStock) {
    }

    private final RestClient http;

    public CatalogoCliente(RestClient.Builder builder, @Value("${orvex.servicios.catalogo-url}") String urlCatalogo) {
        this.http = builder.baseUrl(urlCatalogo).build();
    }

    /** Devuelve solo los productos publicados; los que no existan o no estén publicados no vienen. */
    public List<ProductoCatalogo> productosPublicados(Collection<Long> ids) {
        String lista = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<ProductoCatalogo> r = http.get()
                    .uri(u -> u.path("/api/productos").queryParam("ids", lista).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ProductoCatalogo>>() { });
            return r == null ? List.of() : r;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("catálogo", e);
        }
    }
}
