package com.orvex.auditoria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Auditoría: registro de acciones críticas (M07). */
@SpringBootApplication
@EnableJpaRepositories(considerNestedRepositories = true)
public class AuditoriaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditoriaApplication.class, args);
    }
}
