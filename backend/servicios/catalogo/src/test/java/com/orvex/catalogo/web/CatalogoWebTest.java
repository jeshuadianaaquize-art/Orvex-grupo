package com.orvex.catalogo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orvex.catalogo.servicio.AdminProductoService;
import com.orvex.catalogo.servicio.CatalogoService;
import com.orvex.catalogo.servicio.FotoService;
import com.orvex.common.web.Identidad;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ProductoController.class, AdminProductoController.class})
class CatalogoWebTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    CatalogoService catalogo;
    @MockBean
    AdminProductoService admin;
    @MockBean
    FotoService fotos;

    @Test
    void demasiadosIdsSeRechazan() throws Exception {
        String ids = String.join(",", java.util.Collections.nCopies(ProductoController.MAX_IDS + 1, "1"));
        mvc.perform(get("/api/productos").param("ids", ids))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void categoriaYOrdenInvalidosDanErrorLegible() throws Exception {
        mvc.perform(get("/api/productos").param("categoria", "TOSTADORA")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/productos/buscar").param("orden", "AL_AZAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Valor inválido para el parámetro 'orden'."));
    }

    @Test
    void buscarPasaLosFiltros() throws Exception {
        when(catalogo.buscar(any(), any(), anyInt(), anyInt())).thenReturn(new Dtos.Busqueda(
                new Dtos.Pagina<>(List.of(), 0, 1, 0), new Dtos.Facetas(List.of(), List.of(), List.of(), null), true));
        mvc.perform(get("/api/productos/buscar").param("q", "ryzen").param("marca", "AMD", "Intel").param("disponible", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponibilidadConfirmada").value(true));
    }

    @Test
    void administracionExigeRolAdministradorAunqueLleguePorRutaDirecta() throws Exception {
        mvc.perform(get("/api/catalogo/admin/productos").header(Identidad.USUARIO_ROL, "VENDEDOR"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/catalogo/admin/productos").header(Identidad.USUARIO_ID, "1").header(Identidad.USUARIO_ROL, "CLIENTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"RAM\",\"sku\":\"X\",\"nombre\":\"X\",\"precio\":1}"))
                .andExpect(status().isForbidden());
        verify(admin, never()).crear(any(), any());
    }

    @Test
    void productoInvalidoDevuelveErroresPorCampo() throws Exception {
        mvc.perform(post("/api/catalogo/admin/productos").header(Identidad.USUARIO_ID, "1").header(Identidad.USUARIO_ROL, "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"con espacio\",\"nombre\":\"\",\"precio\":-5,\"especificaciones\":[{\"nombre\":\"Socket CPU\",\"valor\":\"AM4\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.categoria").exists())
                .andExpect(jsonPath("$.campos.sku").exists())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.precio").value("no puede ser negativo"))
                .andExpect(jsonPath("$.campos['especificaciones[0].nombre']").exists());
    }

    @Test
    void fotoInexistenteDa404() throws Exception {
        mvc.perform(get("/api/catalogo/fotos/../../etc/passwd")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/catalogo/fotos/no-existe.png")).andExpect(status().isNotFound());
    }
}
