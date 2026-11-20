package com.orvex.seguridad;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TokenJwtTest {

    private static final String SECRETO = "secreto-de-prueba-de-al-menos-32-bytes!!";
    private final TokenJwt jwt = new TokenJwt(SECRETO);

    @Test
    void sesionValidaConservaLosDatos() {
        String t = jwt.emitirSesion(7, Rol.ADMINISTRADOR, "Ana", Duration.ofMinutes(5));
        var s = jwt.validarSesion(t).orElseThrow();
        assertEquals(7, s.usuarioId());
        assertEquals(Rol.ADMINISTRADOR, s.rol());
        assertEquals("Ana", s.nombre());
        assertNotNull(s.jti());
    }

    @Test
    void cadaSesionTieneUnJtiDistinto() {
        var a = jwt.validarSesion(jwt.emitirSesion(1, Rol.CLIENTE, "x", Duration.ofMinutes(5))).orElseThrow();
        var b = jwt.validarSesion(jwt.emitirSesion(1, Rol.CLIENTE, "x", Duration.ofMinutes(5))).orElseThrow();
        assertNotEquals(a.jti(), b.jti());
    }

    @Test
    void sesionVencidaSeRechaza() {
        var pasado = new TokenJwt(SECRETO, Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC));
        String t = pasado.emitirSesion(1, Rol.CLIENTE, "x", Duration.ofMinutes(30));
        assertTrue(jwt.validarSesion(t).isEmpty());
    }

    @Test
    void firmaDeOtroSecretoSeRechaza() {
        var otro = new TokenJwt("otro-secreto-distinto-de-al-menos-32-bytes");
        String t = otro.emitirSesion(1, Rol.ADMINISTRADOR, "x", Duration.ofMinutes(5));
        assertTrue(jwt.validarSesion(t).isEmpty());
    }

    @Test
    void tokenAlteradoSeRechaza() {
        String t = jwt.emitirSesion(1, Rol.CLIENTE, "x", Duration.ofMinutes(5));
        String alterado = t.substring(0, t.length() - 3) + (t.endsWith("AAA") ? "BBB" : "AAA");
        assertTrue(jwt.validarSesion(alterado).isEmpty());
        assertTrue(jwt.validarSesion("no-es-un-jwt").isEmpty());
        assertTrue(jwt.validarSesion(null).isEmpty());
    }

    @Test
    void tokenDeRecuperacionNoSirveComoSesionNiAlReves() {
        String rec = jwt.emitirRecuperacion(3, "huella", Duration.ofMinutes(30));
        String ses = jwt.emitirSesion(3, Rol.CLIENTE, "x", Duration.ofMinutes(30));
        assertTrue(jwt.validarSesion(rec).isEmpty());
        assertTrue(jwt.validarRecuperacion(ses).isEmpty());
        assertEquals("huella", jwt.validarRecuperacion(rec).orElseThrow().huella());
    }

    @Test
    void secretoCortoNoSeAcepta() {
        assertThrows(IllegalArgumentException.class, () -> new TokenJwt("corto"));
    }
}
