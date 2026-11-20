package com.orvex.gateway.seguridad;

import com.orvex.gateway.seguridad.ReglasAcceso.Publico;
import com.orvex.gateway.seguridad.ReglasAcceso.Requisito;
import com.orvex.gateway.seguridad.ReglasAcceso.Roles;
import com.orvex.seguridad.TokenJwt;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Control de acceso del gateway (RF-24, RNF-04) */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FiltroAutenticacion implements WebFilter {

    public static final String USUARIO_ID = "X-Usuario-Id";
    public static final String USUARIO_ROL = "X-Usuario-Rol";

    private final TokenJwt jwt;
    private final TokensRevocados revocados;
    private final VerificadorAcceso verificador;

    public FiltroAutenticacion(@Value("${orvex.jwt.secreto}") String secreto, TokensRevocados revocados,
                               VerificadorAcceso verificador) {
        this.jwt = new TokenJwt(secreto);
        this.revocados = revocados;
        this.verificador = verificador;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange ex, WebFilterChain cadena) {
        String ruta = ex.getRequest().getPath().value();
        if (!ruta.startsWith("/api/")) {
            return cadena.filter(ex); // actuator del propio gateway
        }
        HttpMethod metodo = ex.getRequest().getMethod();
        Requisito requisito = ReglasAcceso.para(metodo, ruta);
        Optional<TokenJwt.Sesion> sesion = leerToken(ex.getRequest())
                .flatMap(jwt::validarSesion)
                .filter(s -> !revocados.estaRevocado(s.jti()));

        if (sesion.isEmpty()) {
            if (requisito instanceof Publico) {
                return cadena.filter(sinIdentidad(ex));
            }
            return rechazar(ex, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }
        TokenJwt.Sesion s = sesion.get();
        return verificador.verificar(s).flatMap(acceso -> {
            if (acceso.isEmpty() || !acceso.get().activo()) {
                return requisito instanceof Publico
                        ? cadena.filter(sinIdentidad(ex))
                        : rechazar(ex, HttpStatus.UNAUTHORIZED, acceso.isEmpty()
                                ? "Tu sesión ya no es válida. Inicia sesión de nuevo."
                                : "Tu cuenta está bloqueada. Comunícate con la tienda.");
            }
            var a = acceso.get();
            if (requisito instanceof Roles roles && !roles.permitidos().contains(a.rol())) {
                return rechazar(ex, HttpStatus.FORBIDDEN, "No tienes permiso para esta acción.");
            }
            if (HttpMethod.POST.equals(metodo) && "/api/auth/logout".equals(ruta)) {
                revocados.revocar(s.jti(), s.expira());
                verificador.olvidar(s.usuarioId());
            }
            ServerHttpRequest conIdentidad = ex.getRequest().mutate().headers(h -> {
                h.remove(USUARIO_ID);
                h.remove(USUARIO_ROL);
                h.set(USUARIO_ID, Long.toString(a.usuarioId()));
                h.set(USUARIO_ROL, a.rol().name());
            }).build();
            return cadena.filter(ex.mutate().request(conIdentidad).build());
        });
    }

    private static Optional<String> leerToken(ServerHttpRequest r) {
        String h = r.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return h != null && h.regionMatches(true, 0, "Bearer ", 0, 7) ? Optional.of(h.substring(7).trim()) : Optional.empty();
    }

    private static ServerWebExchange sinIdentidad(ServerWebExchange ex) {
        ServerHttpRequest r = ex.getRequest().mutate().headers(h -> {
            h.remove(USUARIO_ID);
            h.remove(USUARIO_ROL);
        }).build();
        return ex.mutate().request(r).build();
    }

    private static Mono<Void> rechazar(ServerWebExchange ex, HttpStatus estado, String mensaje) {
        var resp = ex.getResponse();
        resp.setStatusCode(estado);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"error\":\"" + mensaje.replace("\"", "'") + "\"}";
        return resp.writeWith(Mono.just(resp.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
    }
}
