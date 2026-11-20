package com.orvex.usuarios.servicio;

import com.orvex.usuarios.config.OrvexProps;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Frena ataques de fuerza bruta */
@Component
public class LimitadorIntentos {

    private record Estado(int fallos, Instant bloqueadoHasta) {
    }

    private final Map<String, Estado> porCorreo = new ConcurrentHashMap<>();
    private final int maximo;
    private final Duration bloqueo;
    private final Clock reloj;

    public LimitadorIntentos(OrvexProps props, Clock reloj) {
        this.maximo = props.login().intentosMaximos();
        this.bloqueo = props.login().bloqueo();
        this.reloj = reloj;
    }

    public boolean bloqueado(String correo) {
        Estado e = porCorreo.get(clave(correo));
        return e != null && e.bloqueadoHasta() != null && reloj.instant().isBefore(e.bloqueadoHasta());
    }

    public void registrarFallo(String correo) {
        porCorreo.compute(clave(correo), (k, e) -> {
            int fallos = (e == null || (e.bloqueadoHasta() != null && !reloj.instant().isBefore(e.bloqueadoHasta())))
                    ? 1 : e.fallos() + 1;
            return new Estado(fallos, fallos >= maximo ? reloj.instant().plus(bloqueo) : null);
        });
    }

    public void registrarExito(String correo) {
        porCorreo.remove(clave(correo));
    }

    private static String clave(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
