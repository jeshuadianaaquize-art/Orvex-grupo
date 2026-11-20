package com.orvex.usuarios.servicio;

import com.orvex.common.web.ApiException;
import com.orvex.usuarios.modelo.Direccion;
import com.orvex.usuarios.modelo.Repositorios.DireccionRepository;
import com.orvex.usuarios.modelo.Repositorios.UsuarioRepository;
import com.orvex.usuarios.modelo.Usuario;
import com.orvex.usuarios.web.Dtos;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfil y direcciones de entrega del usuario con sesión (HU-03, RF-02). */
@Service
public class PerfilService {

    static final int MAX_DIRECCIONES = 10;

    private final UsuarioRepository usuarios;
    private final DireccionRepository direcciones;

    public PerfilService(UsuarioRepository usuarios, DireccionRepository direcciones) {
        this.usuarios = usuarios;
        this.direcciones = direcciones;
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.noAutenticado("Tu sesión ya no es válida."));
    }

    @Transactional
    public Usuario actualizar(Long id, Dtos.Perfil p) {
        Usuario u = obtener(id);
        u.actualizarPerfil(p.nombre().trim(), CuentaService.limpio(p.telefono()));
        return u;
    }

    @Transactional(readOnly = true)
    public List<Direccion> direcciones(Long usuarioId) {
        return direcciones.findByUsuarioIdOrderByIdAsc(usuarioId);
    }

    @Transactional
    public Direccion agregarDireccion(Long usuarioId, Dtos.DireccionEntrada d) {
        if (direcciones.countByUsuarioId(usuarioId) >= MAX_DIRECCIONES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Puedes guardar hasta " + MAX_DIRECCIONES + " direcciones.");
        }
        return direcciones.save(new Direccion(usuarioId, d.calle().trim(), d.ciudad().trim(),
                CuentaService.limpio(d.referencia())));
    }

    @Transactional
    public Direccion editarDireccion(Long usuarioId, Long id, Dtos.DireccionEntrada d) {
        Direccion dir = propia(usuarioId, id);
        dir.actualizar(d.calle().trim(), d.ciudad().trim(), CuentaService.limpio(d.referencia()));
        return dir;
    }

    @Transactional
    public void eliminarDireccion(Long usuarioId, Long id) {
        direcciones.delete(propia(usuarioId, id));
    }

    /** Un usuario solo ve y modifica sus propias direcciones (evita acceso por ID ajeno, OWASP A01). */
    private Direccion propia(Long usuarioId, Long id) {
        return direcciones.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> ApiException.noEncontrado("La dirección no existe."));
    }
}
