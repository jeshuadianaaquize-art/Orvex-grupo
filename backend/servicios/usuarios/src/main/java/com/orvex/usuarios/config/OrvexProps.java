package com.orvex.usuarios.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Sección "orvex" de application.yml. Los valores sensibles llegan por variables de entorno. */
@ConfigurationProperties(prefix = "orvex")
public record OrvexProps(Jwt jwt, String webUrl, Correo correo, Login login, Demo demo) {

    public record Jwt(String secreto, Duration duracionSesion, Duration duracionRecuperacion) {
    }

    public record Correo(String remitente) {
    }

    public record Login(int intentosMaximos, Duration bloqueo) {
    }

    public record Demo(boolean habilitado, String contrasena) {
    }
}
