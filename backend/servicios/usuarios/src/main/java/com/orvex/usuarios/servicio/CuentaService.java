package com.orvex.usuarios.servicio;

import com.orvex.common.web.ApiException;
import com.orvex.seguridad.Rol;
import com.orvex.seguridad.TokenJwt;
import com.orvex.usuarios.config.OrvexProps;
import com.orvex.usuarios.modelo.Repositorios.RolRepository;
import com.orvex.usuarios.modelo.Repositorios.UsuarioRepository;
import com.orvex.usuarios.modelo.Usuario;
import com.orvex.usuarios.web.Dtos;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro, inicio de sesión y recuperación de contraseña (HU-01, HU-02, HU-03; RF-01, RF-02). */
@Service
public class CuentaService {

    private static final Logger log = LoggerFactory.getLogger(CuentaService.class);
    static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos.";
    static final String ENLACE_INVALIDO = "El enlace venció o ya se usó. Solicita uno nuevo.";

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder hash;
    private final TokenJwt jwt;
    private final CorreoService correo;
    private final LimitadorIntentos limitador;
    private final OrvexProps props;
    private final String hashRelleno;

    public CuentaService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder hash, TokenJwt jwt,
                         CorreoService correo, LimitadorIntentos limitador, OrvexProps props) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.hash = hash;
        this.jwt = jwt;
        this.correo = correo;
        this.limitador = limitador;
        this.props = props;
        this.hashRelleno = hash.encode("relleno-para-tiempo-constante");
    }

    static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    static String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    @Transactional
    public Usuario registrar(Dtos.Registro r) {
        String email = normalizar(r.correo());
        if (usuarios.existsByCorreoIgnoreCase(email)) {
            throw ApiException.conflicto("correo", "Ya existe una cuenta con este correo.");
        }
        var rolCliente = roles.findByNombre(Rol.CLIENTE.nombreBd()).orElseThrow();
        Usuario u = usuarios.save(new Usuario(rolCliente, r.nombre().trim(), email,
                hash.encode(r.contrasena()), limpio(r.telefono())));
        correo.bienvenida(u.getCorreo(), u.getNombre());
        log.info("Cuenta de cliente creada: id={}", u.getId());
        return u;
    }

    @Transactional(readOnly = true)
    public Dtos.Sesion ingresar(Dtos.Ingreso i) {
        String email = normalizar(i.correo());
        if (limitador.bloqueado(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos. Espera unos minutos antes de volver a intentar.");
        }
        var encontrado = usuarios.findByCorreoIgnoreCase(email);
        boolean ok = hash.matches(i.contrasena(), encontrado.map(Usuario::getContrasenaHash).orElse(hashRelleno));
        if (encontrado.isEmpty() || !ok) {
            limitador.registrarFallo(email);
            throw ApiException.noAutenticado(CREDENCIALES_INVALIDAS);
        }
        Usuario u = encontrado.get();
        if (!u.estaActivo()) {
            // Solo se informa después de comprobar la contraseña: así no revela qué cuentas existen.
            throw ApiException.prohibido("Tu cuenta está bloqueada. Comunícate con la tienda para reactivarla.");
        }
        limitador.registrarExito(email);
        return emitirSesion(u);
    }

    Dtos.Sesion emitirSesion(Usuario u) {
        String token = jwt.emitirSesion(u.getId(), u.getRol().aRol(), u.getNombre(), props.jwt().duracionSesion());
        var sesion = jwt.validarSesion(token).orElseThrow();
        return new Dtos.Sesion(token, sesion.expira(), Dtos.UsuarioDto.de(u));
    }

    /** Responde lo mismo exista o no el correo, para no revelar qué cuentas existen. */
    @Transactional(readOnly = true)
    public void solicitarRecuperacion(Dtos.SolicitudRecuperacion s) {
        usuarios.findByCorreoIgnoreCase(normalizar(s.correo()))
                .filter(Usuario::estaActivo)
                .ifPresent(u -> {
                    var duracion = props.jwt().duracionRecuperacion();
                    String token = jwt.emitirRecuperacion(u.getId(), huella(u.getContrasenaHash()), duracion);
                    correo.recuperacion(u.getCorreo(), u.getNombre(), token, duracion.toMinutes());
                });
    }

    @Transactional
    public void restablecer(Dtos.Restablecer r) {
        var datos = jwt.validarRecuperacion(r.token())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO));
        Usuario u = usuarios.findById(datos.usuarioId())
                .filter(x -> huella(x.getContrasenaHash()).equals(datos.huella()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ENLACE_INVALIDO));
        u.cambiarContrasenaHash(hash.encode(r.contrasena()));
        limitador.registrarExito(u.getCorreo());
        log.info("Contraseña restablecida: id={}", u.getId());
    }

    @Transactional
    public void cambiarContrasena(Long usuarioId, Dtos.CambioContrasena c) {
        Usuario u = usuarios.findById(usuarioId).orElseThrow(() -> ApiException.noEncontrado("La cuenta no existe."));
        if (!hash.matches(c.actual(), u.getContrasenaHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta.",
                    java.util.Map.of("actual", "no coincide con tu contraseña actual"));
        }
        u.cambiarContrasenaHash(hash.encode(c.nueva()));
    }

    /** Resumen del hash actual */
    static String huella(String contrasenaHash) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(contrasenaHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
