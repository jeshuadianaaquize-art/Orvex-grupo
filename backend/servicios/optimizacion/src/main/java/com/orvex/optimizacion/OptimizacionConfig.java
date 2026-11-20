package com.orvex.optimizacion;

import com.orvex.optimizacion.analisis.ParametrosOptimizacion;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ParametrosOptimizacion.class)
public class OptimizacionConfig {

    /** Fechas en UTC, igual que MySQL en Docker. */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
