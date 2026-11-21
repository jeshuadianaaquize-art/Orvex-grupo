package com.orvex.optimizacion.clientes;

import com.orvex.common.web.ApiException;
import com.orvex.common.web.ServicioNoDisponibleException;
import com.orvex.compat.Categoria;
import com.orvex.optimizacion.analisis.UsoPrevisto;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** API interna del servicio de compatibilidad */
@Component
public class CompatibilidadCliente {

    public record Regla(Long id, Categoria categoriaA, Categoria categoriaB, String nombre, String condicion) {
    }

    public record Configuracion(Long id, String nombre, UsoPrevisto uso, List<Long> productoIds) {
    }

    private final RestClient http;

    public CompatibilidadCliente(RestClient.Builder builder, @Value("${orvex.servicios.compatibilidad-url}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    public List<Regla> reglas() {
        try {
            List<Regla> r = http.get().uri("/internal/reglas").retrieve().body(new ParameterizedTypeReference<List<Regla>>() { });
            return r == null ? List.of() : r;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("compatibilidad", e);
        }
    }

    /** 404 si no existe o es de otro usuario. */
    public Configuracion configuracion(Long usuarioId, Long id) {
        try {
            return http.get().uri(u -> u.path("/internal/configuraciones/{id}").queryParam("usuario", usuarioId).build(id))
                    .retrieve().body(Configuracion.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw ApiException.noEncontrado("La configuración no existe.");
            }
            throw new ServicioNoDisponibleException("compatibilidad", e);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("compatibilidad", e);
        }
    }
}
