package com.orvex.compatibilidad;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orvex.common.web.ServicioNoDisponibleException;
import com.orvex.compat.MotorCompatibilidad;
import com.orvex.compat.reglas.ReglasPorDefecto;
import com.orvex.compatibilidad.servicio.ConfiguracionService;
import com.orvex.compatibilidad.servicio.ReglasService;
import com.orvex.compatibilidad.servicio.VerificacionService;
import org.junit.jupiter.api.BeforeEach;
import com.orvex.compat.Categoria;
import com.orvex.compatibilidad.CatalogoCliente.ProductoCatalogo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VerificacionController.class)
@Import(VerificacionService.class)
class VerificacionControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    CatalogoCliente catalogo;
    @MockBean
    ReglasService reglas;
    @MockBean
    ConfiguracionService configuraciones;

    @BeforeEach
    void reglasPorDefecto() {
        when(reglas.motor()).thenReturn(new MotorCompatibilidad(ReglasPorDefecto.crear()));
    }

    private static ProductoCatalogo p(long id, Categoria cat, String precio, Map<String, String> specs) {
        return new ProductoCatalogo(id, "SKU-" + id, "Producto " + id, "Marca", new BigDecimal(precio), null, cat, specs, 5, true);
    }

    private static final ProductoCatalogo CPU = p(1, Categoria.PROCESADOR, "1100.00", Map.of("socket", "AM4", "tdp_w", "65"));
    private static final ProductoCatalogo PLACA_AM4 = p(2, Categoria.PLACA_MADRE, "780.00",
            Map.of("socket", "AM4", "tipo_ram", "DDR4", "factor_forma", "mATX", "interfaces_almacenamiento", "M.2,SATA"));
    private static final ProductoCatalogo PLACA_AM5 = p(3, Categoria.PLACA_MADRE, "980.00",
            Map.of("socket", "AM5", "tipo_ram", "DDR5", "factor_forma", "mATX"));

    private static String cuerpo(String ids) {
        return "{\"productoIds\":[" + ids + "]}";
    }

    private org.springframework.test.web.servlet.ResultActions verificar(String json) throws Exception {
        return mvc.perform(post("/api/armador/verificar").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void armadoCompatibleDevuelveReporteYTotal() throws Exception {
        when(catalogo.productosPublicados(anyCollection())).thenReturn(List.of(CPU, PLACA_AM4));
        verificar(cuerpo("1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporte.global").value("COMPATIBLE"))
                .andExpect(jsonPath("$.precioTotal").value(1880.00))
                .andExpect(jsonPath("$.componentes.length()").value(2));
    }

    @Test
    void socketDistintoEsIncompatible() throws Exception {
        when(catalogo.productosPublicados(anyCollection())).thenReturn(List.of(CPU, PLACA_AM5));
        verificar(cuerpo("1,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporte.global").value("INCOMPATIBLE"));
    }

    @Test
    void productoNoPublicadoOInexistenteSeRechaza() throws Exception {
        when(catalogo.productosPublicados(anyCollection())).thenReturn(List.of(CPU));
        verificar(cuerpo("1,999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void dosPiezasDeLaMismaCategoriaSeRechazan() throws Exception {
        when(catalogo.productosPublicados(anyCollection())).thenReturn(List.of(PLACA_AM4, PLACA_AM5));
        verificar(cuerpo("2,3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El armado solo admite un componente por categoría."));
    }

    @Test
    void lasEspecificacionesDeTextoSeInterpretanParaElMotor() throws Exception {
        var gpu = p(4, Categoria.TARJETA_GRAFICA, "3300.00", Map.of("largo_mm", "340", "tdp_w", "200"));
        var gabinete = p(5, Categoria.GABINETE, "450.00",
                Map.of("factores_forma", "ATX,mATX", "max_largo_gpu_mm", "330", "max_altura_cooler_mm", "160"));
        when(catalogo.productosPublicados(anyCollection())).thenReturn(List.of(gpu, gabinete, PLACA_AM4));
        verificar(cuerpo("4,5,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporte.global").value("INCOMPATIBLE"))
                .andExpect(jsonPath("$.reporte.resultados[?(@.regla=='Formato de placa y gabinete')].severidad").value("COMPATIBLE"))
                .andExpect(jsonPath("$.reporte.resultados[?(@.regla=='Largo de la tarjeta gráfica')].severidad").value("INCOMPATIBLE"));
    }

    @Test
    void catalogoCaidoDevuelve503() throws Exception {
        when(catalogo.productosPublicados(anyCollection()))
                .thenThrow(new ServicioNoDisponibleException("catálogo", new RuntimeException("timeout")));
        verificar(cuerpo("1,2"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void lasRutasDeConfiguracionesExigenSesionYLasDeReglasRolAdministrador() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/configuraciones"))
                .andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/reglas")
                        .header("X-Usuario-Rol", "CLIENTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void menosDeDosComponentesFallaLaValidacion() throws Exception {
        verificar(cuerpo("1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("productoIds: el armado debe tener entre 2 y 16 componentes"));
    }

    @Test
    void idNuloFallaLaValidacion() throws Exception {
        verificar(cuerpo("1,null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void jsonInvalidoDevuelveErrorLegible() throws Exception {
        verificar("{no es json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El cuerpo de la solicitud no es un JSON válido."));
    }
}
