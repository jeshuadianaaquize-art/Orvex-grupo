package com.orvex.optimizacion.clientes;

import com.orvex.common.web.ServicioNoDisponibleException;
import com.orvex.compat.Categoria;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Productos publicados con especificaciones, precio y stock */
@Component
public class CatalogoCliente {

    /** stock y enStock son null si el inventario no respondió */
    public record Producto(Long id, String sku, String nombre, String marca, BigDecimal precio, String fotoUrl,
                           Categoria categoria, Map<String, String> specs, Integer stock, Boolean enStock) {
    }

    private final RestClient http;

    public CatalogoCliente(RestClient.Builder builder, @Value("${orvex.servicios.catalogo-url}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    public List<Producto> publicados() {
        try {
            List<Producto> r = http.get().uri("/api/productos").retrieve()
                    .body(new ParameterizedTypeReference<List<Producto>>() { });
            return r == null ? List.of() : r;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("catálogo", e);
        }
    }
}
