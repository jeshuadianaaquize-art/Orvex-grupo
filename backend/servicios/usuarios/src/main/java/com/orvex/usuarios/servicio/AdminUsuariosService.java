package com.orvex.usuarios.servicio;

import com.orvex.eventos.PublicadorEventos;
import com.orvex.common.web.ApiException;
import com.orvex.seguridad.Rol;
import com.orvex.usuarios.modelo.EstadoUsuario;
import com.orvex.usuarios.modelo.Repositorios.RolRepository;
import com.orvex.usuarios.modelo.Repositorios.UsuarioRepository;
import com.orvex.usuarios.modelo.RolUsuario;
import com.orvex.usuarios.modelo.Usuario;
import com.orvex.usuarios.web.Dtos;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión de usuarios y roles por el administrador (HU-19, RF-21). */
@Service
public class AdminUsuariosService {

    /** Por ahora la auditoría (RF-26) queda en este log con prefijo fijo; en M07 se publica como evento. */
    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");
    static final int TAMANO_PAGINA = 20;

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PublicadorEventos eventos;

    public AdminUsuariosService(UsuarioRepository usuarios, RolRepository roles, PublicadorEventos eventos) {
        this.eventos = eventos;
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public Page<Usuario> buscar(String texto, Rol rol, EstadoUsuario estado, int pagina) {
        return usuarios.buscar(CuentaService.limpio(texto), rol == null ? null : rol.nombreBd(), estado,
                PageRequest.of(Math.max(0, pagina), TAMANO_PAGINA, Sort.by("nombre")));
    }

    @Transactional(readOnly = true)
    public List<RolUsuario> roles() {
        return roles.findAll(Sort.by("id"));
    }

    @Transactional
    public Usuario actualizar(Long adminId, Long id, Dtos.CambioAdmin cambio) {
        if (adminId.equals(id)) {
            // Evita que un administrador se quite el acceso a sí mismo y deje el sistema sin administradores.
            throw ApiException.prohibido("No puedes cambiar tu propio rol ni bloquear tu propia cuenta.");
        }
        Usuario u = usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("El usuario no existe."));
        if (cambio.rol() != null) {
            Rol nuevo = parsear(() -> Rol.desde(cambio.rol()), "rol", "rol desconocido");
            String anterior = u.getRol().getNombre();
            u.cambiarRol(roles.findByNombre(nuevo.nombreBd()).orElseThrow());
            if (!anterior.equals(nuevo.nombreBd())) {
                auditoria.info("accion=cambio_rol usuario_responsable={} usuario_afectado={} anterior={} nuevo={}",
                        adminId, id, anterior, nuevo.nombreBd());
                eventos.publicar("usuario.rol_cambiado", adminId, "usuario", id,
                        Map.of("correo", u.getCorreo(), "anterior", anterior, "nuevo", nuevo.nombreBd()));
            }
        }
        if (cambio.estado() != null) {
            EstadoUsuario nuevo = parsear(() -> EstadoUsuario.valueOf(cambio.estado().trim().toUpperCase()),
                    "estado", "estado desconocido");
            EstadoUsuario anterior = u.getEstado();
            u.cambiarEstado(nuevo);
            if (anterior != nuevo) {
                auditoria.info("accion=cambio_estado usuario_responsable={} usuario_afectado={} anterior={} nuevo={}",
                        adminId, id, anterior, nuevo);
                eventos.publicar("usuario.estado_cambiado", adminId, "usuario", id,
                        Map.of("correo", u.getCorreo(), "anterior", anterior.name(), "nuevo", nuevo.name()));
            }
        }
        return u;
    }

    private static <T> T parsear(java.util.function.Supplier<T> f, String campo, String mensaje) {
        try {
            return f.get();
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, campo + ": " + mensaje, Map.of(campo, mensaje));
        }
    }
}
