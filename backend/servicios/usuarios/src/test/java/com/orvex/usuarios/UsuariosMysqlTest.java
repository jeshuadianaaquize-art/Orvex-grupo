package com.orvex.usuarios;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orvex.eventos.PublicadorEventos;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"orvex.demo.habilitado=true", "orvex.demo.contrasena=Prueba2026x"})
@AutoConfigureMockMvc
@Testcontainers
class UsuariosMysqlTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @MockBean
    JavaMailSender correo;
    @MockBean
    PublicadorEventos eventos;

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    static final AtomicInteger N = new AtomicInteger();

    private long registrar() throws Exception {
        String correoNuevo = "persona" + N.incrementAndGet() + "@correo.test";
        var r = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Ana Quispe","correo":"%s","contrasena":"Clave2026x","telefono":"70000000"}""".formatted(correoNuevo)))
                .andExpect(status().isCreated()).andReturn();
        JsonNode n = json.readTree(r.getResponse().getContentAsString());
        return n.at("/usuario/id").asLong();
    }

    @Test
    void perfilYDirecciones() throws Exception {
        long yo = registrar();
        mvc.perform(get("/api/usuarios/me").header("X-Usuario-Id", yo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
        mvc.perform(put("/api/usuarios/me").header("X-Usuario-Id", yo).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana María Quispe\",\"telefono\":\"71111111\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Ana María Quispe"));
        var r = mvc.perform(post("/api/usuarios/me/direcciones").header("X-Usuario-Id", yo).contentType(MediaType.APPLICATION_JSON)
                .content("{\"calle\":\"Av. Heroínas 123\",\"ciudad\":\"Cochabamba\",\"referencia\":\"Frente a la plaza\"}"))
                .andExpect(status().isCreated()).andReturn();
        long dir = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/usuarios/me/direcciones/" + dir).header("X-Usuario-Id", yo).contentType(MediaType.APPLICATION_JSON)
                .content("{\"calle\":\"Av. Heroínas 125\",\"ciudad\":\"Cochabamba\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/me/direcciones").header("X-Usuario-Id", yo)).andExpect(jsonPath("$[0].calle").value("Av. Heroínas 125"));
        // Pedidos copia la dirección por la API interna; la de otro usuario no existe para él.
        mvc.perform(get("/internal/usuarios/" + yo + "/direcciones/" + dir)).andExpect(status().isOk());
        mvc.perform(get("/internal/usuarios/" + (yo + 999) + "/direcciones/" + dir)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/usuarios/me/direcciones/" + dir).header("X-Usuario-Id", yo)).andExpect(status().isNoContent());
        mvc.perform(post("/api/usuarios/me/direcciones").header("X-Usuario-Id", yo).contentType(MediaType.APPLICATION_JSON)
                .content("{\"calle\":\"\",\"ciudad\":\"\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void elAdministradorCambiaRolYBloqueaConEventoDeAuditoria() throws Exception {
        long u = registrar();
        mvc.perform(patch("/api/usuarios/" + u).header("X-Usuario-Id", 1).header("X-Usuario-Rol", "ADMINISTRADOR")
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("VENDEDOR"));
        verify(eventos).publicar(eq("usuario.rol_cambiado"), eq(1L), eq("usuario"), eq(u), anyMap());
        mvc.perform(patch("/api/usuarios/" + u).header("X-Usuario-Id", 1).header("X-Usuario-Rol", "ADMINISTRADOR")
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"BLOQUEADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("BLOQUEADO"));
        verify(eventos).publicar(eq("usuario.estado_cambiado"), eq(1L), eq("usuario"), eq(u), any());
        mvc.perform(get("/internal/usuarios/" + u + "/acceso")).andExpect(jsonPath("$.estado").value("BLOQUEADO"));
        mvc.perform(patch("/api/usuarios/" + u).header("X-Usuario-Id", 1).header("X-Usuario-Rol", "ADMINISTRADOR")
                .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"PIRATA\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/usuarios?rol=VENDEDOR").header("X-Usuario-Rol", "ADMINISTRADOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[*].id").value(hasItem((int) u)));
        mvc.perform(get("/api/roles").header("X-Usuario-Rol", "ADMINISTRADOR")).andExpect(status().isOk());
    }

    @Test
    void apiInternaParaNotificacionesYAuditoria() throws Exception {
        // Las cuentas de demostración existen (una por rol) y sirven para avisar al personal.
        mvc.perform(get("/internal/usuarios?rol=ALMACENERO,ADMINISTRADOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].correo").value(hasItem("almacenero@orvex.demo")))
                .andExpect(jsonPath("$[*].correo").value(hasItem("administrador@orvex.demo")));
        long u = registrar();
        mvc.perform(get("/internal/usuarios/nombres?ids=" + u)).andExpect(jsonPath("$." + u).value("Ana Quispe"));
        mvc.perform(get("/internal/usuarios/" + u + "/contacto")).andExpect(jsonPath("$.nombre").value("Ana Quispe"));
        mvc.perform(get("/internal/usuarios/999999/contacto")).andExpect(status().isNotFound());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"vendedor@orvex.demo\",\"contrasena\":\"Prueba2026x\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.rol").value("VENDEDOR"));
    }
}
