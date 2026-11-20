package com.orvex.optimizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orvex.common.web.ApiException;
import com.orvex.optimizacion.analisis.ParametrosOptimizacion;
import com.orvex.optimizacion.analisis.UsoPrevisto;
import com.orvex.optimizacion.clientes.CatalogoCliente;
import com.orvex.optimizacion.clientes.CompatibilidadCliente;
import com.orvex.optimizacion.clientes.CompatibilidadCliente.Configuracion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** API y análisis guardados */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OptimizacionMysqlTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @MockBean
    CatalogoCliente catalogo;
    @MockBean
    CompatibilidadCliente compatibilidad;

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ParametrosOptimizacion parametros;

    static final List<Long> INCOMPATIBLE = List.of(1L, 4L, 23L, 26L, 11L, 16L, 12L); // RX 7800 XT con fuente de 450 W

    @BeforeEach
    void preparar() {
        when(catalogo.publicados()).thenReturn(Datos.CATALOGO);
        when(compatibilidad.reglas()).thenReturn(Datos.reglas());
        when(compatibilidad.configuracion(eq(7L), eq(5L))).thenReturn(new Configuracion(5L, "Gamer", null, INCOMPATIBLE));
        when(compatibilidad.configuracion(eq(8L), anyLong())).thenThrow(ApiException.noEncontrado("La configuración no existe."));
    }

    @Test
    void losUmbralesDeApplicationYmlSonLosPropuestosDocumentados() {
        assertEquals(ParametrosOptimizacion.propuestos(), parametros);
    }

    @Test
    void analizarEnVivoNoGuardaNada() throws Exception {
        mvc.perform(post("/api/optimizacion/analizar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoIds\":[1,4,23,26,11,16,12],\"uso\":\"JUEGOS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compatibilidad").value("INCOMPATIBLE"))
                .andExpect(jsonPath("$.buscoAlternativas").value(true))
                .andExpect(jsonPath("$.alternativas[0].producto.id").exists())
                .andExpect(jsonPath("$.recomendaciones[0].explicacion").exists());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM analisis_optimizacion", Integer.class));
    }

    @Test
    void elUsoEsObligatorioYLasGuardadasExigenSesion() throws Exception {
        mvc.perform(post("/api/optimizacion/analizar").contentType(MediaType.APPLICATION_JSON).content("{\"productoIds\":[1,4]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/optimizacion/configuraciones/5")).andExpect(status().isUnauthorized());
    }

    @Test
    void analizarUnaGuardadaLaGuardaUnaSolaVezYConSusAlternativas() throws Exception {
        for (int i = 0; i < 2; i++) { // el segundo análisis reemplaza al primero (UNIQUE id_configuracion)
            mvc.perform(post("/api/optimizacion/configuraciones/5").header("X-Usuario-Id", "7")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"uso\":\"JUEGOS\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uso").value(UsoPrevisto.JUEGOS.name()));
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM analisis_optimizacion WHERE id_configuracion = 5", Integer.class));
        List<Long> sugeridas = jdbc.queryForList("SELECT a.id_producto FROM alternativa_sugerida a "
                + "JOIN analisis_optimizacion o ON o.id_analisis = a.id_analisis WHERE o.id_configuracion = 5", Long.class);
        assertThat(sugeridas).contains(Datos.FTE_650.id(), Datos.FTE_750.id());

        mvc.perform(get("/api/optimizacion/configuraciones/5").header("X-Usuario-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntaje").isNumber())
                .andExpect(jsonPath("$.cuelloBotella").value(org.hamcrest.Matchers.startsWith("Hay piezas incompatibles")))
                .andExpect(jsonPath("$.recomendaciones[0].tipo").value("COMPATIBILIDAD"))
                .andExpect(jsonPath("$.alternativas.length()").value(sugeridas.size()));

        // Otro usuario: 404, igual que en compatibilidad.
        mvc.perform(get("/api/optimizacion/configuraciones/5").header("X-Usuario-Id", "8")).andExpect(status().isNotFound());
        mvc.perform(post("/api/optimizacion/configuraciones/5").header("X-Usuario-Id", "8")).andExpect(status().isNotFound());
    }
}
