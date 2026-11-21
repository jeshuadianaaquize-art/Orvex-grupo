package com.orvex.compatibilidad;

import com.orvex.common.web.ApiException;
import com.orvex.common.web.Identidad;
import com.orvex.compatibilidad.modelo.Modelo.UsoPrevisto;
import com.orvex.compatibilidad.servicio.ConfiguracionService;
import com.orvex.compatibilidad.servicio.ReglasService;
import com.orvex.compatibilidad.servicio.VerificacionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HU-06 / HU-07: verificar un armado */
@RestController
public class VerificacionController {

    public record Solicitud(
            @NotNull(message = "falta la lista de productos")
            @Size(min = 2, max = 16, message = "el armado debe tener entre 2 y 16 componentes")
            List<@NotNull(message = "no se admiten productos vacíos") Long> productoIds) {
    }

    public record Guardar(
            @NotBlank(message = "ponle un nombre") @Size(max = 80, message = "máximo 80 caracteres") String nombre,
            UsoPrevisto uso,
            @NotNull(message = "falta la lista de productos")
            @Size(min = 2, max = 16, message = "el armado debe tener entre 2 y 16 componentes")
            List<@NotNull(message = "no se admiten productos vacíos") Long> productoIds) {
    }

    private final VerificacionService verificacion;
    private final ConfiguracionService configuraciones;
    private final ReglasService reglas;

    public VerificacionController(VerificacionService verificacion, ConfiguracionService configuraciones, ReglasService reglas) {
        this.verificacion = verificacion;
        this.configuraciones = configuraciones;
        this.reglas = reglas;
    }

    @PostMapping("/api/armador/verificar")
    public VerificacionService.Resultado verificar(@Valid @RequestBody Solicitud solicitud) {
        return verificacion.verificar(solicitud.productoIds());
    }

    // configuraciones guardadas

    @GetMapping("/api/configuraciones")
    public List<ConfiguracionService.Resumen> misConfiguraciones(@RequestHeader(Identidad.USUARIO_ID) Long yo) {
        return configuraciones.listar(yo);
    }

    @PostMapping("/api/configuraciones")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfiguracionService.Detalle guardar(@RequestHeader(Identidad.USUARIO_ID) Long yo, @Valid @RequestBody Guardar g) {
        return configuraciones.guardar(yo, g.nombre(), g.uso(), g.productoIds());
    }

    @GetMapping("/api/configuraciones/{id}")
    public ConfiguracionService.Detalle abrir(@RequestHeader(Identidad.USUARIO_ID) Long yo, @PathVariable Long id) {
        return configuraciones.abrir(yo, id);
    }

    @PutMapping("/api/configuraciones/{id}")
    public ConfiguracionService.Detalle actualizar(@RequestHeader(Identidad.USUARIO_ID) Long yo, @PathVariable Long id,
                                                   @Valid @RequestBody Guardar g) {
        return configuraciones.actualizar(yo, id, g.nombre(), g.uso(), g.productoIds());
    }

    @DeleteMapping("/api/configuraciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@RequestHeader(Identidad.USUARIO_ID) Long yo, @PathVariable Long id) {
        configuraciones.eliminar(yo, id);
    }

    // reglas (administrador)

    @GetMapping("/api/reglas")
    public List<ReglasService.ReglaDto> reglas(@RequestHeader(Identidad.USUARIO_ROL) String rol) {
        exigirAdmin(rol);
        return reglas.listar();
    }

    @PostMapping("/api/reglas")
    @ResponseStatus(HttpStatus.CREATED)
    public ReglasService.ReglaDto crearRegla(@RequestHeader(Identidad.USUARIO_ID) Long yo,
                                             @RequestHeader(Identidad.USUARIO_ROL) String rol,
                                             @RequestBody ReglasService.Entrada e) {
        exigirAdmin(rol);
        return reglas.crear(yo, e);
    }

    @PutMapping("/api/reglas/{id}")
    public ReglasService.ReglaDto editarRegla(@RequestHeader(Identidad.USUARIO_ID) Long yo,
                                              @RequestHeader(Identidad.USUARIO_ROL) String rol,
                                              @PathVariable Long id, @RequestBody ReglasService.Entrada e) {
        exigirAdmin(rol);
        return reglas.actualizar(yo, id, e);
    }

    @DeleteMapping("/api/reglas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarRegla(@RequestHeader(Identidad.USUARIO_ID) Long yo, @RequestHeader(Identidad.USUARIO_ROL) String rol,
                              @PathVariable Long id) {
        exigirAdmin(rol);
        reglas.eliminar(yo, id);
    }

    private static void exigirAdmin(String rol) {
        if (!"ADMINISTRADOR".equals(rol)) {
            throw ApiException.prohibido("Necesitas el rol de administrador.");
        }
    }
}
