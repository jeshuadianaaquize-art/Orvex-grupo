package com.orvex.gateway.seguridad;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Tokens invalidados al cerrar sesión (caso de uso "Cerrar sesión": el sistema invalida el token). */
@Component
public class TokensRevocados {

    private final Map<String, Instant> revocados = new ConcurrentHashMap<>();
    private final Clock reloj;

    public TokensRevocados() {
        this(Clock.systemUTC());
    }

    TokensRevocados(Clock reloj) {
        this.reloj = reloj;
    }

    public void revocar(String jti, Instant venceEl) {
        Instant ahora = reloj.instant();
        revocados.values().removeIf(v -> v.isBefore(ahora));
        revocados.put(jti, venceEl);
    }

    public boolean estaRevocado(String jti) {
        return revocados.containsKey(jti);
    }
}
