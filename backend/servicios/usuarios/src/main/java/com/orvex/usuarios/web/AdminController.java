package com.orvex.usuarios.web;

import com.orvex.common.web.ApiException;
import com.orvex.common.web.Identidad;
import com.orvex.seguridad.Rol;
import com.orvex.usuarios.modelo.EstadoUsuario;
import com.orvex.usuarios.servicio.AdminUsuariosService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de usuarios y roles (HU-19) */
@RestController
public class AdminController {

    private final AdminUsuariosService admin;

    public AdminController(AdminUsuariosService admin) {
        this.admin = admin;
    }

    @GetMapping("/api/usuarios")
    public Dtos.Pagina<Dtos.UsuarioDto> listar(@RequestHeader(Identidad.USUARIO_ROL) String rolActual,
                                               @RequestParam(required = false) String buscar,
                                               @RequestParam(required = false) Rol rol,
                                               @RequestParam(required = false) EstadoUsuario estado,
                                               @RequestParam(defaultValue = "0") int pagina) {
        exigirAdmin(rolActual);
        var p = admin.buscar(buscar, rol, estado, pagina);
        return new Dtos.Pagina<>(p.getContent().stream().map(Dtos.UsuarioDto::de).toList(),
                p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    @PatchMapping("/api/usuarios/{id}")
    public Dtos.UsuarioDto actualizar(@RequestHeader(Identidad.USUARIO_ID) Long yo,
                                      @RequestHeader(Identidad.USUARIO_ROL) String rolActual,
                                      @PathVariable Long id, @RequestBody Dtos.CambioAdmin cambio) {
        exigirAdmin(rolActual);
        return Dtos.UsuarioDto.de(admin.actualizar(yo, id, cambio));
    }

    @GetMapping("/api/roles")
    public List<Dtos.RolDto> roles(@RequestHeader(Identidad.USUARIO_ROL) String rolActual) {
        exigirAdmin(rolActual);
        return admin.roles().stream().map(r -> new Dtos.RolDto(r.aRol().name(), r.getDescripcion())).toList();
    }

    private static void exigirAdmin(String rol) {
        if (!Rol.ADMINISTRADOR.name().equals(rol)) {
            throw ApiException.prohibido("Necesitas el rol de administrador.");
        }
    }
}
