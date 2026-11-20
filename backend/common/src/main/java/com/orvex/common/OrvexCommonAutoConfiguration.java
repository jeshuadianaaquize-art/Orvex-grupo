package com.orvex.common;

import com.orvex.common.resiliencia.CircuitoPorServicio;
import com.orvex.common.web.ApiExceptionHandler;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

/** Se activa sola en cada microservicio que dependa de "common". */
@AutoConfiguration
@Import(ApiExceptionHandler.class)
public class OrvexCommonAutoConfiguration {

    /** Un circuito por servicio destino, compartido por todos los clientes del servicio. */
    @Bean
    @ConditionalOnMissingBean
    CircuitBreakerRegistry orvexCircuitos(ObjectProvider<MeterRegistry> metricas) {
        CircuitBreakerRegistry r = CircuitoPorServicio.registroPorDefecto();
        // Estado de cada circuito en /actuator/prometheus
        metricas.ifAvailable(m -> TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(r).bindTo(m));
        return r;
    }

    /** prototype: el builder es mutable (baseUrl); cada cliente recibe uno propio y no pisa la URL de otro. */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean
    RestClient.Builder orvexRestClientBuilder(
            @Value("${orvex.http.connect-timeout:2s}") Duration conexion,
            @Value("${orvex.http.read-timeout:5s}") Duration lectura,
            CircuitBreakerRegistry circuitos) {
        var ajustes = ClientHttpRequestFactorySettings.DEFAULTS.withConnectTimeout(conexion).withReadTimeout(lectura);
        return RestClient.builder().requestFactory(ClientHttpRequestFactories.get(ajustes))
                .requestInterceptor(new CircuitoPorServicio(circuitos));
    }
}
