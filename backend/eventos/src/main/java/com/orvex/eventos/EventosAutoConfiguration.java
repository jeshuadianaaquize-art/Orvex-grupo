package com.orvex.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Se activa sola en los servicios que dependen de "eventos". */
@AutoConfiguration(before = RabbitAutoConfiguration.class)
public class EventosAutoConfiguration {

    @Bean
    TopicExchange orvexEventos() {
        return new TopicExchange(PublicadorEventos.EXCHANGE, true, false);
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    MessageConverter orvexConversorJson(ObjectMapper json) {
        return new Jackson2JsonMessageConverter(json);
    }

    @Bean
    PublicadorEventos publicadorEventos(RabbitTemplate rabbit, @Value("${spring.application.name:servicio}") String servicio,
                                        @Value("${orvex.eventos.habilitado:false}") boolean habilitado) {
        return new PublicadorEventos(rabbit, servicio, habilitado);
    }

    /** Valores por defecto: RabbitMQ caído no debe marcar al servicio como caído */
    public static class Valores implements EnvironmentPostProcessor {
        @Override
        public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
            env.getPropertySources().addLast(new MapPropertySource("orvex-eventos", Map.of(
                    "management.health.rabbit.enabled", "false",
                    "spring.rabbitmq.host", "${RABBIT_HOST:localhost}",
                    "spring.rabbitmq.username", "${RABBIT_USUARIO:orvex}",
                    "spring.rabbitmq.password", "${RABBIT_CLAVE:orvex_dev}",
                    "spring.rabbitmq.connection-timeout", "2s",
                    "spring.rabbitmq.template.retry.enabled", "false",
                    "orvex.eventos.habilitado", "${EVENTOS_HABILITADOS:false}")));
        }
    }
}
