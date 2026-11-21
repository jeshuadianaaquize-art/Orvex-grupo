package com.orvex.compatibilidad;

import com.orvex.compatibilidad.servicio.ConfiguracionService;
import com.orvex.compatibilidad.servicio.ReglasService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API interna para el servicio de optimización (M05) */
@RestController
@RequestMapping("/internal")
public class InternoController {

    private final ReglasService reglas;
    private final ConfiguracionService configuraciones;

    public InternoController(ReglasService reglas, ConfiguracionService configuraciones) {
        this.reglas = reglas;
        this.configuraciones = configuraciones;
    }

    /** Reglas vigentes: optimización las usa para que cada alternativa vuelva a pasar la compatibilidad. */
    @GetMapping("/reglas")
    public List<ReglasService.ReglaDto> reglas() {
        return reglas.listar();
    }

    /** Configuración de un usuario; 404 si no existe o es de otro (mismo criterio que la API pública). */
    @GetMapping("/configuraciones/{id}")
    public ConfiguracionService.Interna configuracion(@PathVariable Long id, @RequestParam Long usuario) {
        return configuraciones.interna(usuario, id);
    }
}
