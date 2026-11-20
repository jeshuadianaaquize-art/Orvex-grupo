package com.orvex.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Notificaciones: correos automáticos desde la cola de mensajes (M07). */
@SpringBootApplication
public class NotificacionesApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificacionesApplication.class, args);
    }
}
