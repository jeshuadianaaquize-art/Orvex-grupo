package com.orvex.catalogo.web;

import com.orvex.catalogo.servicio.AdminProductoService;
import com.orvex.catalogo.servicio.FotoService;
import com.orvex.common.web.ApiException;
import com.orvex.common.web.Identidad;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Administración de productos (HU-05, RF-05) */
@RestController
@RequestMapping("/api/catalogo/admin")
public class AdminProductoController {

    private final AdminProductoService admin;
    private final FotoService fotos;

    public AdminProductoController(AdminProductoService admin, FotoService fotos) {
        this.admin = admin;
        this.fotos = fotos;
    }

    @GetMapping("/productos")
    public List<Dtos.ProductoAdminDto> listar(@RequestHeader(Identidad.USUARIO_ROL) String rol) {
        exigirAdmin(rol);
        return admin.listar();
    }

    @GetMapping("/productos/{id}")
    public Dtos.ProductoAdminDto obtener(@RequestHeader(Identidad.USUARIO_ROL) String rol, @PathVariable Long id) {
        exigirAdmin(rol);
        return admin.obtener(id);
    }

    @PostMapping("/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.ProductoAdminDto crear(@RequestHeader(Identidad.USUARIO_ID) Long yo,
                                       @RequestHeader(Identidad.USUARIO_ROL) String rol,
                                       @Valid @RequestBody Dtos.ProductoEntrada e) {
        exigirAdmin(rol);
        return admin.crear(yo, e);
    }

    @PutMapping("/productos/{id}")
    public Dtos.ProductoAdminDto actualizar(@RequestHeader(Identidad.USUARIO_ID) Long yo,
                                            @RequestHeader(Identidad.USUARIO_ROL) String rol,
                                            @PathVariable Long id, @Valid @RequestBody Dtos.ProductoEntrada e) {
        exigirAdmin(rol);
        return admin.actualizar(yo, id, e);
    }

    @PatchMapping("/productos/{id}/estado")
    public Dtos.ProductoAdminDto estado(@RequestHeader(Identidad.USUARIO_ID) Long yo,
                                        @RequestHeader(Identidad.USUARIO_ROL) String rol,
                                        @PathVariable Long id, @Valid @RequestBody Dtos.CambioEstado c) {
        exigirAdmin(rol);
        return admin.cambiarEstado(yo, id, c.estado());
    }

    @PostMapping("/productos/{id}/foto")
    public Dtos.ProductoAdminDto foto(@RequestHeader(Identidad.USUARIO_ROL) String rol, @PathVariable Long id,
                                      @RequestParam("archivo") MultipartFile archivo) {
        exigirAdmin(rol);
        String anterior = admin.fotoActual(id);
        var r = admin.cambiarFoto(id, fotos.guardar(id, archivo));
        fotos.borrar(anterior);
        return r;
    }

    @GetMapping("/especificaciones")
    public List<Dtos.CategoriaSpecsDto> especificaciones(@RequestHeader(Identidad.USUARIO_ROL) String rol) {
        exigirAdmin(rol);
        return admin.catalogoDeEspecificaciones();
    }

    private static void exigirAdmin(String rol) {
        if (!"ADMINISTRADOR".equals(rol)) {
            throw ApiException.prohibido("Necesitas el rol de administrador.");
        }
    }
}
