package com.orvex.catalogo;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Configuración aparte para que @WebMvcTest no cree repositorios */
@Configuration
@EnableJpaRepositories(basePackages = "com.orvex.catalogo.modelo", considerNestedRepositories = true)
public class CatalogoConfig {
}
