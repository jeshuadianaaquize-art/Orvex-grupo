package com.orvex.common.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Formato único de errores {"error", "campos"} para todos los servicios. */
class ApiExceptionHandlerTest {

    record Entrada(@NotBlank(message = "escribe el nombre") String nombre, @NotBlank(message = "escribe la ciudad") String ciudad) {
    }

    @RestController
    static class Prueba {
        @GetMapping("/negocio")
        void negocio() {
            throw ApiException.conflicto("sku", "ese SKU ya existe");
        }

        @GetMapping("/argumento")
        void argumento() {
            throw new IllegalArgumentException("rango inválido");
        }

        @PostMapping("/validar")
        void validar(@Valid @RequestBody Entrada e) {
        }

        @GetMapping("/numero/{n}")
        void numero(@PathVariable Long n) {
        }

        @GetMapping("/sesion")
        void sesion(@RequestHeader(Identidad.USUARIO_ID) Long yo) {
        }

        @GetMapping("/archivo")
        void archivo() {
            throw new MaxUploadSizeExceededException(2_000_000);
        }

        @GetMapping("/caido")
        void caido() {
            throw new ServicioNoDisponibleException("catálogo", new RuntimeException("conexión rechazada"));
        }

        @GetMapping("/otros")
        void otros() {
            throw ApiException.noEncontrado("no existe");
        }
    }

    MockMvc mvc = MockMvcBuilders.standaloneSetup(new Prueba()).setControllerAdvice(new ApiExceptionHandler()).build();

    @Test
    void cadaErrorTieneSuEstadoYMensaje() throws Exception {
        mvc.perform(get("/negocio")).andExpect(status().isConflict()).andExpect(jsonPath("$.campos.sku").value("ese SKU ya existe"));
        mvc.perform(get("/argumento")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("rango inválido"));
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\",\"ciudad\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Revisa los datos marcados."))
                .andExpect(jsonPath("$.campos.ciudad").value("escribe la ciudad"));
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana\",\"ciudad\":\"\"}"))
                .andExpect(jsonPath("$.error").value("ciudad: escribe la ciudad"));
        mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/numero/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Valor inválido para el parámetro 'n'."));
        mvc.perform(get("/sesion")).andExpect(status().isUnauthorized());
        mvc.perform(get("/archivo")).andExpect(status().isPayloadTooLarge());
        mvc.perform(get("/caido")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/otros")).andExpect(status().isNotFound());
    }

    @Test
    void fabricas() {
        org.junit.jupiter.api.Assertions.assertEquals(403, ApiException.prohibido("x").getEstado().value());
        org.junit.jupiter.api.Assertions.assertEquals(401, ApiException.noAutenticado("x").getEstado().value());
        org.junit.jupiter.api.Assertions.assertEquals(Map.of(), new ApiException(org.springframework.http.HttpStatus.CONFLICT, "x").getCampos());
    }
}
