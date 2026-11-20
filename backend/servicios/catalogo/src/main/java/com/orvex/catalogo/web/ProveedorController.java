package com.orvex.catalogo.web;

import com.orvex.catalogo.modelo.Proveedor;
import com.orvex.catalogo.modelo.Repositorios.ProveedorRepository;
import com.orvex.common.web.ApiException;
import com.orvex.common.web.Identidad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Proveedores (RF-14). Los gestionan el almacenero y el administrador. */
@RestController
public class ProveedorController {

    private static final Set<String> ROLES = Set.of("ALMACENERO", "ADMINISTRADOR");

    public record ProveedorDto(Long id, String nombre, String contacto) {
        static ProveedorDto de(Proveedor p) {
            return new ProveedorDto(p.getId(), p.getNombre(), p.getContacto());
        }
    }

    public record EntradaProveedor(
            @NotBlank(message = "escribe el nombre") @Size(max = 80, message = "máximo 80 caracteres") String nombre,
            @Size(max = 100, message = "máximo 100 caracteres") String contacto) {
    }

    private final ProveedorRepository proveedores;

    public ProveedorController(ProveedorRepository proveedores) {
        this.proveedores = proveedores;
    }

    @GetMapping("/api/proveedores")
    @Transactional(readOnly = true)
    public List<ProveedorDto> listar(@RequestHeader(Identidad.USUARIO_ROL) String rol) {
        exigir(rol);
        return proveedores.findAllByOrderByNombreAsc().stream().map(ProveedorDto::de).toList();
    }

    @PostMapping("/api/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProveedorDto crear(@RequestHeader(Identidad.USUARIO_ROL) String rol, @Valid @RequestBody EntradaProveedor e) {
        exigir(rol);
        if (proveedores.existsByNombreIgnoreCase(e.nombre().trim())) {
            throw ApiException.conflicto("nombre", "Ya existe un proveedor con ese nombre.");
        }
        return ProveedorDto.de(proveedores.save(new Proveedor(e.nombre().trim(), limpio(e.contacto()))));
    }

    @PutMapping("/api/proveedores/{id}")
    @Transactional
    public ProveedorDto editar(@RequestHeader(Identidad.USUARIO_ROL) String rol, @PathVariable Long id,
                               @Valid @RequestBody EntradaProveedor e) {
        exigir(rol);
        Proveedor p = proveedores.findById(id).orElseThrow(() -> ApiException.noEncontrado("El proveedor no existe."));
        if (proveedores.existsByNombreIgnoreCaseAndIdNot(e.nombre().trim(), id)) {
            throw ApiException.conflicto("nombre", "Ya existe un proveedor con ese nombre.");
        }
        p.actualizar(e.nombre().trim(), limpio(e.contacto()));
        return ProveedorDto.de(p);
    }

    private static void exigir(String rol) {
        if (!ROLES.contains(rol)) {
            throw ApiException.prohibido("Necesitas el rol de almacenero o administrador.");
        }
    }

    private static String limpio(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
