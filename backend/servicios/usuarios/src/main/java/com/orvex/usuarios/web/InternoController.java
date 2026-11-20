package com.orvex.usuarios.web;

import com.orvex.usuarios.modelo.Repositorios.DireccionRepository;
import com.orvex.usuarios.modelo.Repositorios.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** API interna: solo la usa el gateway desde la red de Docker */
@RestController
public class InternoController {

    /** Dirección de entrega con el nombre y correo del cliente (para el pedido y su recibo). */
    public record Entrega(Long direccionId, String calle, String ciudad, String referencia) {
    }

    public record Contacto(Long id, String nombre, String correo) {
    }

    private final UsuarioRepository usuarios;
    private final DireccionRepository direcciones;

    public InternoController(UsuarioRepository usuarios, DireccionRepository direcciones) {
        this.usuarios = usuarios;
        this.direcciones = direcciones;
    }

    /** 404 si la dirección no existe o es de otro usuario: pedidos la copia al confirmar la compra. */
    @GetMapping("/internal/usuarios/{id}/direcciones/{direccionId}")
    public ResponseEntity<Entrega> direccion(@PathVariable Long id, @PathVariable Long direccionId) {
        return direcciones.findByIdAndUsuarioId(direccionId, id)
                .map(d -> ResponseEntity.ok(new Entrega(d.getId(), d.getCalle(), d.getCiudad(), d.getReferencia())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Nombre y correo del cliente para el recibo y, más adelante, las notificaciones. */
    @GetMapping("/internal/usuarios/{id}/contacto")
    public ResponseEntity<Contacto> contacto(@PathVariable Long id) {
        return usuarios.findById(id)
                .map(u -> ResponseEntity.ok(new Contacto(u.getId(), u.getNombre(), u.getCorreo())))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Personal activo de los roles pedidos */
    @GetMapping("/internal/usuarios")
    public java.util.List<Contacto> porRol(@org.springframework.web.bind.annotation.RequestParam java.util.List<String> rol) {
        var roles = rol.stream().map(r -> com.orvex.seguridad.Rol.desde(r).nombreBd()).toList();
        return usuarios.findByRol_NombreInAndEstado(roles, com.orvex.usuarios.modelo.EstadoUsuario.ACTIVO).stream()
                .map(u -> new Contacto(u.getId(), u.getNombre(), u.getCorreo())).toList();
    }

    /** Nombres para mostrar quién hizo cada acción en la auditoría. */
    @GetMapping("/internal/usuarios/nombres")
    public java.util.Map<Long, String> nombres(@org.springframework.web.bind.annotation.RequestParam java.util.List<Long> ids) {
        if (ids.size() > 200) {
            throw new IllegalArgumentException("Máximo 200 usuarios por consulta.");
        }
        java.util.Map<Long, String> r = new java.util.HashMap<>();
        usuarios.findAllById(ids).forEach(u -> r.put(u.getId(), u.getNombre()));
        return r;
    }

    @GetMapping("/internal/usuarios/{id}/acceso")
    public ResponseEntity<Dtos.Acceso> acceso(@PathVariable Long id) {
        return usuarios.findById(id)
                .map(u -> ResponseEntity.ok(new Dtos.Acceso(u.getId(), u.getRol().aRol().name(), u.getEstado().name())))
                .orElse(ResponseEntity.notFound().build());
    }
}
