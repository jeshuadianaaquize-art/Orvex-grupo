package com.orvex.usuarios.config;

import com.orvex.seguridad.TokenJwt;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableAsync
@EnableConfigurationProperties(OrvexProps.class)
@EnableJpaRepositories(basePackages = "com.orvex.usuarios.modelo", considerNestedRepositories = true)
public class UsuariosConfig {

    @Bean
    TokenJwt tokenJwt(OrvexProps props) {
        return new TokenJwt(props.jwt().secreto());
    }

    /** bcrypt con costo 10 (RNF-04: contraseñas con hash). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
