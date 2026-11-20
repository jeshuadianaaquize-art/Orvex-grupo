package com.orvex.usuarios.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orvex.common.web.ApiException;
import com.orvex.seguridad.Rol;
import com.orvex.seguridad.TokenJwt;
import com.orvex.usuarios.config.OrvexProps;
import com.orvex.usuarios.modelo.EstadoUsuario;
import com.orvex.usuarios.modelo.Repositorios.RolRepository;
import com.orvex.usuarios.modelo.Repositorios.UsuarioRepository;
import com.orvex.usuarios.modelo.RolUsuario;
import com.orvex.usuarios.modelo.Usuario;
import com.orvex.usuarios.web.Dtos;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class CuentaServiceTest {

    static final OrvexProps PROPS = new OrvexProps(
            new OrvexProps.Jwt("secreto-de-prueba-de-al-menos-32-bytes!!", Duration.ofMinutes(60), Duration.ofMinutes(30)),
            "http://localhost:3000", new OrvexProps.Correo("test@orvex.local"),
            new OrvexProps.Login(5, Duration.ofMinutes(15)), new OrvexProps.Demo(false, "x"));

    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    RolRepository roles = mock(RolRepository.class);
    CorreoService correo = mock(CorreoService.class);
    BCryptPasswordEncoder hash = new BCryptPasswordEncoder(4);
    TokenJwt jwt = new TokenJwt(PROPS.jwt().secreto());
    LimitadorIntentos limitador = new LimitadorIntentos(PROPS, Clock.systemUTC());
    CuentaService cuentas;

    @BeforeEach
    void preparar() {
        cuentas = new CuentaService(usuarios, roles, hash, jwt, correo, limitador, PROPS);
    }

    static RolUsuario rol(String nombre) throws Exception {
        var ctor = RolUsuario.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        RolUsuario r = ctor.newInstance();
        Field f = RolUsuario.class.getDeclaredField("nombre");
        f.setAccessible(true);
        f.set(r, nombre);
        return r;
    }

    Usuario usuario(long id, String contrasena) throws Exception {
        Usuario u = new Usuario(rol("cliente"), "Ana", "ana@correo.com", hash.encode(contrasena), null);
        Field f = Usuario.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(u, id);
        when(usuarios.findByCorreoIgnoreCase("ana@correo.com")).thenReturn(Optional.of(u));
        when(usuarios.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void registroGuardaHashYEnviaBienvenida() throws Exception {
        when(roles.findByNombre("cliente")).thenReturn(Optional.of(rol("cliente")));
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
        cuentas.registrar(new Dtos.Registro(" Ana ", " ANA@Correo.com ", "secreta123", ""));
        var guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(guardado.capture());
        assertEquals("ana@correo.com", guardado.getValue().getCorreo());
        assertNotEquals("secreta123", guardado.getValue().getContrasenaHash());
        assertTrue(hash.matches("secreta123", guardado.getValue().getContrasenaHash()));
        assertNull(guardado.getValue().getTelefono());
        verify(correo).bienvenida("ana@correo.com", "Ana");
    }

    @Test
    void correoRepetidoDaConflictoEnElCampoCorreo() {
        when(usuarios.existsByCorreoIgnoreCase("ana@correo.com")).thenReturn(true);
        var e = assertThrows(ApiException.class,
                () -> cuentas.registrar(new Dtos.Registro("Ana", "ana@correo.com", "secreta123", null)));
        assertEquals(HttpStatus.CONFLICT, e.getEstado());
        assertTrue(e.getCampos().containsKey("correo"));
    }

    @Test
    void loginCorrectoDevuelveTokenConRol() throws Exception {
        usuario(9, "secreta123");
        var s = cuentas.ingresar(new Dtos.Ingreso("ana@correo.com", "secreta123"));
        var datos = jwt.validarSesion(s.token()).orElseThrow();
        assertEquals(9, datos.usuarioId());
        assertEquals(Rol.CLIENTE, datos.rol());
    }

    @Test
    void contrasenaIncorrectaYCorreoInexistenteDanElMismoMensaje() throws Exception {
        usuario(9, "secreta123");
        var mal = assertThrows(ApiException.class, () -> cuentas.ingresar(new Dtos.Ingreso("ana@correo.com", "otra12345")));
        var noExiste = assertThrows(ApiException.class, () -> cuentas.ingresar(new Dtos.Ingreso("nadie@x.com", "otra12345")));
        assertEquals(HttpStatus.UNAUTHORIZED, mal.getEstado());
        assertEquals(mal.getMessage(), noExiste.getMessage());
    }

    @Test
    void cuentaBloqueadaNoIniciaSesion() throws Exception {
        usuario(9, "secreta123").cambiarEstado(EstadoUsuario.BLOQUEADO);
        var e = assertThrows(ApiException.class, () -> cuentas.ingresar(new Dtos.Ingreso("ana@correo.com", "secreta123")));
        assertEquals(HttpStatus.FORBIDDEN, e.getEstado());
    }

    @Test
    void cincoFallosBloqueanTemporalmenteAunConLaContrasenaCorrecta() throws Exception {
        usuario(9, "secreta123");
        for (int i = 0; i < 5; i++) {
            assertThrows(ApiException.class, () -> cuentas.ingresar(new Dtos.Ingreso("ana@correo.com", "mala12345")));
        }
        var e = assertThrows(ApiException.class, () -> cuentas.ingresar(new Dtos.Ingreso("ana@correo.com", "secreta123")));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getEstado());
    }

    @Test
    void recuperacionDeCorreoInexistenteNoEnviaNadaNiFalla() {
        assertDoesNotThrow(() -> cuentas.solicitarRecuperacion(new Dtos.SolicitudRecuperacion("nadie@x.com")));
        verify(correo, never()).recuperacion(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void enlaceDeRecuperacionSirveUnaSolaVez() throws Exception {
        usuario(9, "secreta123");
        cuentas.solicitarRecuperacion(new Dtos.SolicitudRecuperacion("ana@correo.com"));
        var token = ArgumentCaptor.forClass(String.class);
        verify(correo).recuperacion(eq("ana@correo.com"), eq("Ana"), token.capture(), eq(30L));

        cuentas.restablecer(new Dtos.Restablecer(token.getValue(), "nueva12345"));
        assertTrue(cuentas.ingresar(new Dtos.Ingreso("ana@correo.com", "nueva12345")).token() != null);

        var e = assertThrows(ApiException.class,
                () -> cuentas.restablecer(new Dtos.Restablecer(token.getValue(), "otra12345")));
        assertEquals(CuentaService.ENLACE_INVALIDO, e.getMessage());
    }

    @Test
    void enlaceFalsoSeRechaza() {
        var e = assertThrows(ApiException.class, () -> cuentas.restablecer(new Dtos.Restablecer("falso", "nueva12345")));
        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
    }

    @Test
    void cambiarContrasenaExigeLaActual() throws Exception {
        usuario(9, "secreta123");
        var e = assertThrows(ApiException.class,
                () -> cuentas.cambiarContrasena(9L, new Dtos.CambioContrasena("equivocada1", "nueva12345")));
        assertTrue(e.getCampos().containsKey("actual"));
        cuentas.cambiarContrasena(9L, new Dtos.CambioContrasena("secreta123", "nueva12345"));
        assertNotNull(cuentas.ingresar(new Dtos.Ingreso("ana@correo.com", "nueva12345")));
    }
}
