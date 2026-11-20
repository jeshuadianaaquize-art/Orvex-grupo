package com.orvex.usuarios.web;

import com.orvex.usuarios.modelo.Direccion;
import com.orvex.usuarios.modelo.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;

/** Contratos JSON del servicio de usuarios. Los mensajes de validación se muestran junto a cada campo. */
public final class Dtos {

    private Dtos() {
    }

    /** Contraseña: 8 a 72 caracteres (límite de bcrypt), con al menos una letra y un número. */
    static final String REGEX_CONTRASENA = "^(?=.*[A-Za-zÁÉÍÓÚáéíóúÑñ])(?=.*\\d).{8,72}$";
    static final String MSJ_CONTRASENA = "debe tener entre 8 y 72 caracteres, con al menos una letra y un número";
    static final String REGEX_TELEFONO = "^$|^\\+?[0-9 ]{7,20}$";

    public record Registro(
            @NotBlank(message = "escribe tu nombre") @Size(max = 80, message = "máximo 80 caracteres") String nombre,
            @NotBlank(message = "escribe tu correo") @Email(message = "no es un correo válido")
            @Size(max = 100, message = "máximo 100 caracteres") String correo,
            @NotBlank(message = "elige una contraseña") @Pattern(regexp = REGEX_CONTRASENA, message = MSJ_CONTRASENA) String contrasena,
            @Pattern(regexp = REGEX_TELEFONO, message = "solo números, entre 7 y 20 dígitos") String telefono) {
    }

    public record Ingreso(
            @NotBlank(message = "escribe tu correo") String correo,
            @NotBlank(message = "escribe tu contraseña") String contrasena) {
    }

    public record SolicitudRecuperacion(
            @NotBlank(message = "escribe tu correo") @Email(message = "no es un correo válido") String correo) {
    }

    public record Restablecer(
            @NotBlank(message = "falta el enlace de recuperación") String token,
            @NotBlank(message = "elige una contraseña") @Pattern(regexp = REGEX_CONTRASENA, message = MSJ_CONTRASENA) String contrasena) {
    }

    public record CambioContrasena(
            @NotBlank(message = "escribe tu contraseña actual") String actual,
            @NotBlank(message = "elige una contraseña") @Pattern(regexp = REGEX_CONTRASENA, message = MSJ_CONTRASENA) String nueva) {
    }

    public record Perfil(
            @NotBlank(message = "escribe tu nombre") @Size(max = 80, message = "máximo 80 caracteres") String nombre,
            @Pattern(regexp = REGEX_TELEFONO, message = "solo números, entre 7 y 20 dígitos") String telefono) {
    }

    public record DireccionEntrada(
            @NotBlank(message = "escribe la calle y el número") @Size(max = 120, message = "máximo 120 caracteres") String calle,
            @NotBlank(message = "escribe la ciudad") @Size(max = 50, message = "máximo 50 caracteres") String ciudad,
            @Size(max = 150, message = "máximo 150 caracteres") String referencia) {
    }

    public record CambioAdmin(String rol, String estado) {
    }

    public record UsuarioDto(Long id, String nombre, String correo, String telefono, String rol, String estado,
                             LocalDateTime fechaRegistro) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(),
                    u.getRol().aRol().name(), u.getEstado().name(), u.getFechaRegistro());
        }
    }

    public record DireccionDto(Long id, String calle, String ciudad, String referencia) {
        public static DireccionDto de(Direccion d) {
            return new DireccionDto(d.getId(), d.getCalle(), d.getCiudad(), d.getReferencia());
        }
    }

    public record Sesion(String token, Instant expira, UsuarioDto usuario) {
    }

    public record Mensaje(String mensaje) {
    }

    /** Lo que el gateway consulta antes de dejar pasar una petición con sesión. */
    public record Acceso(Long id, String rol, String estado) {
    }

    public record RolDto(String nombre, String descripcion) {
    }

    public record Pagina<T>(java.util.List<T> elementos, int pagina, int totalPaginas, long total) {
    }
}
