package com.orvex.auditoria;

import com.orvex.common.web.ApiException;
import com.orvex.common.web.Identidad;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de la auditoría (solo administrador) */
@RestController
public class AuditoriaController {

    private final AuditoriaService auditoria;
    private final ZoneId zona;

    public AuditoriaController(AuditoriaService auditoria, @Value("${orvex.zona-horaria:America/La_Paz}") String zona) {
        this.auditoria = auditoria;
        this.zona = ZoneId.of(zona);
    }

    @GetMapping("/api/auditoria")
    public AuditoriaService.Pagina buscar(@RequestHeader(Identidad.USUARIO_ROL) String rol,
                                          @RequestParam(required = false) String accion,
                                          @RequestParam(required = false) String entidad,
                                          @RequestParam(required = false) Long usuarioId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                          @RequestParam(defaultValue = "0") int pagina) {
        admin(rol);
        return auditoria.buscar(accion, entidad, usuarioId,
                desde == null ? null : desde.atStartOfDay(zona).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(zona).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                pagina);
    }

    @GetMapping("/api/auditoria/acciones")
    public List<String> acciones(@RequestHeader(Identidad.USUARIO_ROL) String rol) {
        admin(rol);
        return auditoria.acciones();
    }

    private static void admin(String rol) {
        if (!"ADMINISTRADOR".equals(rol)) {
            throw ApiException.prohibido("Solo el administrador puede ver la auditoría.");
        }
    }
}
