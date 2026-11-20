package com.orvex.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.orvex.seguridad.Rol;
import com.orvex.seguridad.TokenJwt;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "15s")
class GatewayTest {

    static final String SECRETO = "secreto-de-prueba-del-gateway-de-32-bytes!";
    static final TokenJwt JWT = new TokenJwt(SECRETO);

    /** Usuarios de prueba según el servicio de usuarios simulado: id → {rol, estado}. */
    static final Map<String, String> CUENTAS = Map.of(
            "1", "{\"id\":1,\"rol\":\"ADMINISTRADOR\",\"estado\":\"ACTIVO\"}",
            "2", "{\"id\":2,\"rol\":\"CLIENTE\",\"estado\":\"ACTIVO\"}",
            "3", "{\"id\":3,\"rol\":\"CLIENTE\",\"estado\":\"BLOQUEADO\"}",
            // El token dice CLIENTE pero el administrador ya le cambió el rol: manda el rol actual.
            "4", "{\"id\":4,\"rol\":\"ADMINISTRADOR\",\"estado\":\"ACTIVO\"}");

    static MockWebServer catalogo = new MockWebServer();
    static MockWebServer usuarios = new MockWebServer();

    @BeforeAll
    static void iniciar() throws IOException {
        catalogo.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                if ("/actuator/health".equals(r.getPath())) {
                    return json("{\"status\":\"UP\"}");
                }
                if (r.getPath() != null && r.getPath().startsWith("/api/productos")) {
                    return json("[{\"id\":1,\"nombre\":\"Ryzen 5 5600\",\"ruta\":\"" + r.getPath() + "\"}]");
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        usuarios.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest r) {
                String p = r.getPath() == null ? "" : r.getPath();
                if (p.startsWith("/internal/usuarios/")) {
                    String id = p.split("/")[3];
                    return CUENTAS.containsKey(id) ? json(CUENTAS.get(id)) : new MockResponse().setResponseCode(404);
                }
                if (p.equals("/actuator/health")) {
                    return json("{\"status\":\"UP\"}");
                }
                if (p.startsWith("/api/auth/logout")) {
                    return new MockResponse().setResponseCode(204);
                }
                // Devuelve la identidad que recibió, para comprobar lo que reenvía el gateway.
                return json("{\"id\":\"" + r.getHeader("X-Usuario-Id") + "\",\"rol\":\"" + r.getHeader("X-Usuario-Rol") + "\"}");
            }
        });
        catalogo.start();
        usuarios.start();
    }

    @AfterAll
    static void detener() throws IOException {
        catalogo.shutdown();
        usuarios.shutdown();
    }

    private static MockResponse json(String cuerpo) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(cuerpo);
    }

    @DynamicPropertySource
    static void servicios(DynamicPropertyRegistry r) {
        r.add("orvex.jwt.secreto", () -> SECRETO);
        r.add("CATALOGO_URL", () -> "http://localhost:" + catalogo.getPort());
        r.add("USUARIOS_URL", () -> "http://localhost:" + usuarios.getPort());
        // Puerto cerrado: simula el servicio de optimización caído.
        r.add("OPTIMIZACION_URL", () -> "http://localhost:1");
    }

    @Autowired
    WebTestClient web;

    @LocalServerPort
    int puerto;

    static String token(long id, Rol rol) {
        return "Bearer " + JWT.emitirSesion(id, rol, "Prueba", Duration.ofMinutes(10));
    }

    // enrutamiento

    @Test
    void catalogoEsPublicoYConservaLaConsulta() {
        web.get().uri("/api/productos?categoria=PROCESADOR").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].nombre").isEqualTo("Ryzen 5 5600")
                .jsonPath("$[0].ruta").isEqualTo("/api/productos?categoria=PROCESADOR");
    }

    @Test
    void rutaDesconocidaSinSesionPideIniciarSesion() {
        web.get().uri("/api/no-existe").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void servicioCaidoDevuelveErrorSinColgarse() {
        web.get().uri("/api/optimizacion/x").header("Authorization", token(2, Rol.CLIENTE))
                .exchange().expectStatus().is5xxServerError();
    }

    // autenticación y roles

    @Test
    void perfilSinTokenDa401ConMensaje() {
        web.get().uri("/api/usuarios/me").exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("Debes iniciar sesión.");
    }

    @Test
    void tokenInvalidoOFirmadoConOtroSecretoDa401() {
        var otro = new TokenJwt("otro-secreto-que-no-es-el-del-gateway-32b");
        web.get().uri("/api/usuarios/me").header("Authorization", "Bearer basura").exchange().expectStatus().isUnauthorized();
        web.get().uri("/api/usuarios/me")
                .header("Authorization", "Bearer " + otro.emitirSesion(1, Rol.ADMINISTRADOR, "x", Duration.ofMinutes(5)))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void conSesionElGatewayReenviaLaIdentidad() {
        web.get().uri("/api/usuarios/me").header("Authorization", token(2, Rol.CLIENTE)).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo("2").jsonPath("$.rol").isEqualTo("CLIENTE");
    }

    @Test
    void cabecerasDeIdentidadFalsificadasSeDescartan() {
        // Un cliente intenta hacerse pasar por el administrador 1 mandando las cabeceras a mano.
        web.get().uri("/api/usuarios/me").header("Authorization", token(2, Rol.CLIENTE))
                .header("X-Usuario-Id", "1").header("X-Usuario-Rol", "ADMINISTRADOR").exchange()
                .expectBody().jsonPath("$.id").isEqualTo("2").jsonPath("$.rol").isEqualTo("CLIENTE");
        // Sin token, las cabeceras falsas no abren una ruta con sesión.
        web.get().uri("/api/usuarios/me").header("X-Usuario-Id", "1").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void clienteNoEntraARutasDeAdministrador() {
        web.get().uri("/api/usuarios").header("Authorization", token(2, Rol.CLIENTE)).exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.error").isEqualTo("No tienes permiso para esta acción.");
        web.get().uri("/api/estado").header("Authorization", token(2, Rol.CLIENTE)).exchange().expectStatus().isForbidden();
    }

    @Test
    void administracionDelCatalogoEsSoloParaAdministradores() {
        web.post().uri("/api/catalogo/admin/productos").exchange().expectStatus().isUnauthorized();
        web.get().uri("/api/catalogo/admin/productos").header("Authorization", token(2, Rol.CLIENTE))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void inventarioEsParaAlmaceneroYAdministrador() {
        web.get().uri("/api/inventario/existencias").header("Authorization", token(2, Rol.CLIENTE))
                .exchange().expectStatus().isForbidden();
        web.get().uri("/api/proveedores").header("Authorization", token(2, Rol.CLIENTE))
                .exchange().expectStatus().isForbidden();
        // Las reservas no tienen ruta pública: ni con sesión se llega a ellas.
        web.post().uri("/api/reservas").header("Authorization", token(1, Rol.ADMINISTRADOR))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void administradorSiEntra() {
        web.get().uri("/api/usuarios").header("Authorization", token(1, Rol.ADMINISTRADOR)).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.rol").isEqualTo("ADMINISTRADOR");
    }

    @Test
    void cuentaBloqueadaPierdeElAccesoAunqueSuTokenSigaVigente() {
        web.get().uri("/api/usuarios/me").header("Authorization", token(3, Rol.CLIENTE)).exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.error").isEqualTo("Tu cuenta está bloqueada. Comunícate con la tienda.");
    }

    @Test
    void mandaElRolActualNoElDelToken() {
        web.get().uri("/api/usuarios").header("Authorization", token(4, Rol.CLIENTE)).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.rol").isEqualTo("ADMINISTRADOR");
    }

    @Test
    void usuarioEliminadoPierdeLaSesion() {
        web.get().uri("/api/usuarios/me").header("Authorization", token(99, Rol.CLIENTE)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void cerrarSesionInvalidaElToken() {
        String t = token(2, Rol.CLIENTE);
        web.get().uri("/api/usuarios/me").header("Authorization", t).exchange().expectStatus().isOk();
        web.post().uri("/api/auth/logout").header("Authorization", t).exchange().expectStatus().isNoContent();
        web.get().uri("/api/usuarios/me").header("Authorization", t).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void estadoMarcaDegradadoSiUnServicioCae() {
        web.get().uri("/api/estado").header("Authorization", token(1, Rol.ADMINISTRADOR)).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("DEGRADADO")
                .jsonPath("$.servicios[?(@.servicio=='catalogo')].estado").isEqualTo("UP")
                .jsonPath("$.servicios[?(@.servicio=='optimizacion')].estado").isEqualTo("DOWN");
    }

    // CORS

    /** Con un cliente HTTP real: el WebTestClient no envía el Origin igual que un navegador. */
    private HttpResponse<String> pedir(String metodo, String ruta, String origen, String... cabeceras) throws Exception {
        var b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .method(metodo, HttpRequest.BodyPublishers.noBody())
                .header("Origin", origen);
        for (int i = 0; i < cabeceras.length; i += 2) {
            b.header(cabeceras[i], cabeceras[i + 1]);
        }
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void corsPermiteLaWebYRechazaOtrosOrigenes() throws Exception {
        var ok = pedir("OPTIONS", "/api/usuarios/me", "http://localhost:3000",
                "Access-Control-Request-Method", "GET", "Access-Control-Request-Headers", "authorization");
        assertThat(ok.statusCode()).isEqualTo(200);
        assertThat(ok.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:3000");
        assertThat(pedir("OPTIONS", "/api/productos", "http://sitio-ajeno.com", "Access-Control-Request-Method", "GET")
                .statusCode()).isEqualTo(403);
    }

    @Test
    void elRechazo401TambienLlevaCorsParaQueLaWebLeaElMensaje() throws Exception {
        var r = pedir("GET", "/api/usuarios/me", "http://localhost:3000");
        assertThat(r.statusCode()).isEqualTo(401);
        assertThat(r.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:3000");
        assertThat(r.body()).contains("Debes iniciar sesión.");
    }
}
