package com.orvex.gateway;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Nombre de cada microservicio y su URL interna (sección orvex.servicios de application.yml). */
@ConfigurationProperties(prefix = "orvex")
public record ServiciosProperties(Map<String, String> servicios) {
    public ServiciosProperties {
        servicios = servicios == null ? Map.of() : Map.copyOf(servicios);
    }
}
