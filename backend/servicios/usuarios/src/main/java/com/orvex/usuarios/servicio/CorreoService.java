package com.orvex.usuarios.servicio;

import com.orvex.usuarios.config.OrvexProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Correos de la cuenta (RF-20): bienvenida y recuperación de contraseña. */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final JavaMailSender mail;
    private final OrvexProps props;

    public CorreoService(JavaMailSender mail, OrvexProps props) {
        this.mail = mail;
        this.props = props;
    }

    @Async
    public void bienvenida(String correo, String nombre) {
        enviar(correo, "Bienvenido a Orvex", """
                Hola %s:

                Tu cuenta de Orvex está lista. Ya puedes armar tu PC, verificar la compatibilidad
                de las piezas y seguir tus pedidos.

                Ingresa en: %s/ingresar

                — Equipo Orvex
                """.formatted(nombre, props.webUrl()));
    }

    @Async
    public void recuperacion(String correo, String nombre, String token, long minutos) {
        enviar(correo, "Restablece tu contraseña de Orvex", """
                Hola %s:

                Recibimos una solicitud para restablecer tu contraseña. Abre este enlace para elegir una nueva:

                %s/restablecer?token=%s

                El enlace vence en %d minutos y sirve una sola vez.
                Si no fuiste tú, ignora este correo: tu contraseña no cambiará.

                — Equipo Orvex
                """.formatted(nombre, props.webUrl(), token, minutos));
    }

    private void enviar(String para, String asunto, String texto) {
        try {
            var m = new SimpleMailMessage();
            m.setFrom(props.correo().remitente());
            m.setTo(para);
            m.setSubject(asunto);
            m.setText(texto);
            mail.send(m);
            log.info("Correo '{}' enviado", asunto);
        } catch (MailException e) {
            // No se registra la dirección completa en los logs (dato personal).
            log.warn("No se pudo enviar el correo '{}': {}", asunto, e.getMessage());
        }
    }
}
