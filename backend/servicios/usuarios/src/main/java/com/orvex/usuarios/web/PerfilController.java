package com.orvex.usuarios.web;

import com.orvex.common.web.Identidad;
import com.orvex.usuarios.servicio.CuentaService;
import com.orvex.usuarios.servicio.PerfilService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Perfil y direcciones del usuario con sesión */
@RestController
@RequestMapping("/api/usuarios/me")
public class PerfilController {

    private final PerfilService perfil;
    private final CuentaService cuentas;

    public PerfilController(PerfilService perfil, CuentaService cuentas) {
        this.perfil = perfil;
        this.cuentas = cuentas;
    }

    @GetMapping
    public Dtos.UsuarioDto ver(@RequestHeader(Identidad.USUARIO_ID) Long yo) {
        return Dtos.UsuarioDto.de(perfil.obtener(yo));
    }

    @PutMapping
    public Dtos.UsuarioDto actualizar(@RequestHeader(Identidad.USUARIO_ID) Long yo, @Valid @RequestBody Dtos.Perfil p) {
        return Dtos.UsuarioDto.de(perfil.actualizar(yo, p));
    }

    @PostMapping("/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(@RequestHeader(Identidad.USUARIO_ID) Long yo, @Valid @RequestBody Dtos.CambioContrasena c) {
        cuentas.cambiarContrasena(yo, c);
    }

    @GetMapping("/direcciones")
    public List<Dtos.DireccionDto> direcciones(@RequestHeader(Identidad.USUARIO_ID) Long yo) {
        return perfil.direcciones(yo).stream().map(Dtos.DireccionDto::de).toList();
    }

    @PostMapping("/direcciones")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.DireccionDto agregar(@RequestHeader(Identidad.USUARIO_ID) Long yo, @Valid @RequestBody Dtos.DireccionEntrada d) {
        return Dtos.DireccionDto.de(perfil.agregarDireccion(yo, d));
    }

    @PutMapping("/direcciones/{id}")
    public Dtos.DireccionDto editar(@RequestHeader(Identidad.USUARIO_ID) Long yo, @PathVariable Long id,
                                    @Valid @RequestBody Dtos.DireccionEntrada d) {
        return Dtos.DireccionDto.de(perfil.editarDireccion(yo, id, d));
    }

    @DeleteMapping("/direcciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestHeader(Identidad.USUARIO_ID) Long yo, @PathVariable Long id) {
        perfil.eliminarDireccion(yo, id);
    }
}
