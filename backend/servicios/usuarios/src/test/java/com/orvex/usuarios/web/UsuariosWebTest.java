package com.orvex.usuarios.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orvex.common.web.ApiException;
import com.orvex.common.web.Identidad;
import com.orvex.usuarios.servicio.AdminUsuariosService;
import com.orvex.usuarios.servicio.CuentaService;
import com.orvex.usuarios.servicio.PerfilService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, PerfilController.class, AdminController.class})
class UsuariosWebTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    CuentaService cuentas;
    @MockBean
    PerfilService perfil;
    @MockBean
    AdminUsuariosService admin;

    @Test
    void registroInvalidoDevuelveErroresPorCampo() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"correo\":\"no-es-correo\",\"contrasena\":\"corta\",\"telefono\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.correo").value("no es un correo válido"))
                .andExpect(jsonPath("$.campos.contrasena").exists())
                .andExpect(jsonPath("$.campos.telefono").exists());
        verify(cuentas, never()).registrar(any());
    }

    @Test
    void contrasenaSinNumeroSeRechaza() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"correo\":\"ana@x.com\",\"contrasena\":\"solamenteletras\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.contrasena").exists());
    }

    @Test
    void credencialesInvalidasDan401() throws Exception {
        when(cuentas.ingresar(any())).thenThrow(ApiException.noAutenticado("Correo o contraseña incorrectos."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@x.com\",\"contrasena\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Correo o contraseña incorrectos."));
    }

    @Test
    void recuperarSiempreRespondeElMismoMensaje() throws Exception {
        mvc.perform(post("/api/auth/recuperar").contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"nadie@x.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Si el correo está registrado, te enviamos un enlace para restablecer la contraseña."));
    }

    @Test
    void perfilSinIdentidadDelGatewayDa401() throws Exception {
        mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeListarUsuariosNiConLaRutaDirecta() throws Exception {
        mvc.perform(get("/api/usuarios").header(Identidad.USUARIO_ID, "5").header(Identidad.USUARIO_ROL, "CLIENTE"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/7").header(Identidad.USUARIO_ID, "5").header(Identidad.USUARIO_ROL, "VENDEDOR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isForbidden());
        verify(admin, never()).actualizar(eq(5L), eq(7L), any());
    }
}
