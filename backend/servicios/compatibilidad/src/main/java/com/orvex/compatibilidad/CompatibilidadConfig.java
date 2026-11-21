package com.orvex.compatibilidad;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** En su propia clase para que las pruebas de la capa web (@WebMvcTest) no intenten crear repositorios. */
@Configuration
@EnableJpaRepositories(basePackages = "com.orvex.compatibilidad.modelo", considerNestedRepositories = true)
public class CompatibilidadConfig {

    /** UTC, igual que MySQL en Docker. */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
