package com.orvex.notificaciones;

import com.orvex.common.web.ApiException;
import com.orvex.common.web.Identidad;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Avisos del usuario con sesión y resumen del envío (administrador). */
@RestController
public class NotificacionesController {

    @Configuration
    @EnableScheduling
    @EnableJpaRepositories(basePackageClasses = NotificacionesService.class, considerNestedRepositories = true)
    static class Config {
        @Bean
        Clock reloj() {
            return Clock.systemUTC();
        }
    }

    private final NotificacionesService notificaciones;

    public NotificacionesController(NotificacionesService notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping("/api/notificaciones")
    public List<NotificacionesService.Vista> mias(@RequestHeader(Identidad.USUARIO_ID) Long yo) {
        return notificaciones.mias(yo);
    }

    @GetMapping("/api/notificaciones/resumen")
    public NotificacionesService.Resumen resumen(@RequestHeader(Identidad.USUARIO_ROL) String rol) {
        if (!"ADMINISTRADOR".equals(rol)) {
            throw ApiException.prohibido("Solo el administrador puede ver el estado de los envíos.");
        }
        return notificaciones.resumen();
    }
}
