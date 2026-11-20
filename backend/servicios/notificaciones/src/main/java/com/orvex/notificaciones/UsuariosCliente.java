package com.orvex.notificaciones;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Destinatarios de los avisos: API interna del servicio de usuarios. */
@Component
public class UsuariosCliente {

    private static final Logger log = LoggerFactory.getLogger(UsuariosCliente.class);

    public record Contacto(Long id, String nombre, String correo) {
    }

    private final RestClient http;
    private volatile boolean disponible = true;

    public UsuariosCliente(RestClient.Builder b, @Value("${orvex.servicios.usuarios-url:http://localhost:8101}") String url) {
        this.http = b.baseUrl(url).build();
    }

    /** Vacío si el usuario no existe o si el servicio no respondió (ver {@link #disponible()}). */
    public Optional<Contacto> contacto(Long id) {
        try {
            Contacto c = http.get().uri("/internal/usuarios/{id}/contacto", id).retrieve().body(Contacto.class);
            disponible = true;
            return Optional.ofNullable(c);
        } catch (HttpClientErrorException e) {
            disponible = true;
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            log.warn("usuarios respondió {} para el contacto {}", e.getStatusCode(), id);
            return Optional.empty();
        } catch (RestClientException e) {
            disponible = false;
            return Optional.empty();
        }
    }

    /** Personal activo con esos roles */
    public List<Contacto> personal(List<String> roles) {
        List<Contacto> r = http.get().uri(u -> u.path("/internal/usuarios").queryParam("rol", String.join(",", roles)).build())
                .retrieve().body(new ParameterizedTypeReference<List<Contacto>>() { });
        return r == null ? List.of() : r;
    }

    /** false si la última consulta no llegó al servicio de usuarios. */
    public boolean disponible() {
        return disponible;
    }
}
