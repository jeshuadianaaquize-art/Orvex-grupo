package com.orvex.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;

/** Tokens firmados con HMAC-SHA256 (RNF-04: JWT con expiración). */
public final class TokenJwt {

    public static final int LARGO_MINIMO_SECRETO = 32;
    private static final String EMISOR = "orvex";
    private static final String TIPO_SESION = "sesion";
    private static final String TIPO_RECUPERACION = "recuperacion";

    public record Sesion(long usuarioId, Rol rol, String nombre, String jti, Instant expira) {
    }

    public record Recuperacion(long usuarioId, String huella) {
    }

    private final SecretKey clave;
    private final Clock reloj;

    public TokenJwt(String secreto) {
        this(secreto, Clock.systemUTC());
    }

    public TokenJwt(String secreto, Clock reloj) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LARGO_MINIMO_SECRETO) {
            throw new IllegalArgumentException(
                    "JWT_SECRET debe tener al menos " + LARGO_MINIMO_SECRETO + " caracteres.");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.reloj = reloj;
    }

    public String emitirSesion(long usuarioId, Rol rol, String nombre, Duration duracion) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(Long.toString(usuarioId))
                .id(UUID.randomUUID().toString())
                .claim("tipo", TIPO_SESION)
                .claim("rol", rol.name())
                .claim("nombre", nombre)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    /** Vacío si el token no es válido, está vencido, fue alterado o no es de sesión. */
    public Optional<Sesion> validarSesion(String token) {
        return leer(token, TIPO_SESION).map(c -> new Sesion(
                Long.parseLong(c.getSubject()),
                Rol.valueOf(c.get("rol", String.class)),
                c.get("nombre", String.class),
                c.getId(),
                c.getExpiration().toInstant()));
    }

    public String emitirRecuperacion(long usuarioId, String huella, Duration duracion) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .issuer(EMISOR)
                .subject(Long.toString(usuarioId))
                .claim("tipo", TIPO_RECUPERACION)
                .claim("huella", huella)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    public Optional<Recuperacion> validarRecuperacion(String token) {
        return leer(token, TIPO_RECUPERACION)
                .map(c -> new Recuperacion(Long.parseLong(c.getSubject()), c.get("huella", String.class)));
    }

    private Optional<Claims> leer(String token, String tipo) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims c = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(EMISOR)
                    .clock(() -> Date.from(reloj.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return tipo.equals(c.get("tipo", String.class)) ? Optional.of(c) : Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
