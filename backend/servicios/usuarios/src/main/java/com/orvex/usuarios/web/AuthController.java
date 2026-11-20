package com.orvex.usuarios.web;

import com.orvex.usuarios.servicio.CuentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Rutas públicas de la cuenta (HU-01, HU-02, HU-03). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CuentaService cuentas;

    public AuthController(CuentaService cuentas) {
        this.cuentas = cuentas;
    }

    /** Crea la cuenta con rol Cliente y devuelve la sesión ya iniciada. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.Sesion registrar(@Valid @RequestBody Dtos.Registro r) {
        var u = cuentas.registrar(r);
        return cuentas.ingresar(new Dtos.Ingreso(u.getCorreo(), r.contrasena()));
    }

    @PostMapping("/login")
    public Dtos.Sesion ingresar(@Valid @RequestBody Dtos.Ingreso i) {
        return cuentas.ingresar(i);
    }

    /** El gateway revoca el token antes de llegar aquí; este servicio no guarda sesiones. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void salir() {
        // Sin estado en este servicio: la revocación vive en el gateway.
    }

    @PostMapping("/recuperar")
    public Dtos.Mensaje recuperar(@Valid @RequestBody Dtos.SolicitudRecuperacion s) {
        cuentas.solicitarRecuperacion(s);
        return new Dtos.Mensaje("Si el correo está registrado, te enviamos un enlace para restablecer la contraseña.");
    }

    @PostMapping("/restablecer")
    public Dtos.Mensaje restablecer(@Valid @RequestBody Dtos.Restablecer r) {
        cuentas.restablecer(r);
        return new Dtos.Mensaje("Tu contraseña se actualizó. Ya puedes iniciar sesión.");
    }
}
