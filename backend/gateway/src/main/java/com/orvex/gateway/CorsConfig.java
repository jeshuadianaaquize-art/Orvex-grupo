package com.orvex.gateway;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/** CORS como filtro previo a la autenticación: así también los rechazos 401/403 llevan las cabeceras */
@Configuration
public class CorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    CorsWebFilter corsWebFilter(@Value("${CORS_ORIGINS:http://localhost:3000}") String origenes) {
        var c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).filter(o -> !o.isEmpty()).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        c.setMaxAge(Duration.ofHours(1));
        var fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", c);
        return new CorsWebFilter(fuente);
    }
}
