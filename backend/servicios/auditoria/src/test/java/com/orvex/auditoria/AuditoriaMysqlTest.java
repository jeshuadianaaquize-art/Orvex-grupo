package com.orvex.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.orvex.eventos.Evento;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Registro de acciones críticas desde los eventos, idempotente, contra MySQL 8 real. */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Testcontainers
class AuditoriaMysqlTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    AuditoriaService auditoria;
    @Autowired
    MockMvc mvc;

    @Test
    void registraUnCambioDePrecioUnaSolaVez() {
        Evento e = Evento.nuevo("producto.precio_cambiado", "catalogo", 1L, "producto", 7,
                Map.of("anterior", new BigDecimal("100.00"), "nuevo", new BigDecimal("90.00")));
        assertTrue(auditoria.registrar(e));
        assertFalse(auditoria.registrar(e), "una entrega repetida de la cola no duplica el registro");
        var p = auditoria.buscar("producto.precio_cambiado", null, 1L, null, null, 0);
        assertEquals(1, p.total());
        var r = p.elementos().get(0);
        assertEquals("producto", r.entidad());
        assertThat(r.detalle()).contains("\"anterior\":100.00").contains("\"id\":\"7\"").contains("catalogo");
    }

    @Test
    void accionesDelSistemaSinUsuarioYFiltros() {
        auditoria.registrar(Evento.nuevo("pago.conciliacion", "pedidos-pagos", null, "pago", "REF-1", Map.of("motivo", "monto distinto")));
        auditoria.registrar(Evento.nuevo("venta.anulada", "pedidos-pagos", 3L, "venta", 15, Map.of("motivo", "error")));
        assertThat(auditoria.buscar(null, "venta", null, null, null, 0).elementos())
                .allMatch(r -> r.entidad().equals("venta"));
        assertThat(auditoria.acciones()).contains("pago.conciliacion", "venta.anulada");
        assertThat(auditoria.buscar("pago.conciliacion", null, null, null, null, 0).elementos().get(0).usuarioId()).isNull();
    }

    @Test
    void soloElAdministradorConsulta() throws Exception {
        mvc.perform(get("/api/auditoria").header("X-Usuario-Rol", "ALMACENERO")).andExpect(status().isForbidden());
        mvc.perform(get("/api/auditoria").header("X-Usuario-Rol", "ADMINISTRADOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos").isArray());
    }
}
