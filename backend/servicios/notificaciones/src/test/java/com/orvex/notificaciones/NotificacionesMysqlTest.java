package com.orvex.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orvex.eventos.Evento;
import com.orvex.notificaciones.Notificacion.Estado;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Eventos → avisos, envío con reintentos y Circuit Breaker */
@SpringBootTest(properties = {"spring.rabbitmq.listener.simple.auto-startup=false", "orvex.notificaciones.envio-ms=3600000"})
@Testcontainers
class NotificacionesMysqlTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @MockBean
    JavaMailSender correo;
    @MockBean
    UsuariosCliente usuarios;

    @Autowired
    NotificacionesService servicio;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        reset(correo, usuarios);
        jdbc.update("DELETE FROM notificacion");
        servicio.circuito().transitionToClosedState();
        when(usuarios.contacto(anyLong())).thenAnswer(i -> Optional.of(new UsuariosCliente.Contacto(i.getArgument(0), "Ana",
                "ana" + i.getArgument(0) + "@correo.test")));
        when(usuarios.disponible()).thenReturn(true);
        when(usuarios.personal(anyList())).thenReturn(List.of(new UsuariosCliente.Contacto(2L, "Alma", "alma@correo.test"),
                new UsuariosCliente.Contacto(3L, "Admin", "admin@correo.test")));
    }

    private static Evento pago(long cliente, String canal) {
        Map<String, Object> d = new HashMap<>(Map.of("clienteId", cliente, "canal", canal, "total", "1250.5", "recibo", "R-00000009"));
        return Evento.nuevo("pago.confirmado", "pedidos-pagos", null, "venta", 9, d);
    }

    @Test
    void elPagoConfirmadoAvisaAlClienteUnaSolaVezYSeEnvia() {
        Evento e = pago(10, "LINEA");
        assertEquals(1, servicio.procesar(e));
        assertEquals(0, servicio.procesar(e), "entrega repetida de la cola");
        assertEquals(1, servicio.enviarPendientes());
        var c = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo).send(c.capture());
        assertThat(c.getValue().getTo()).containsExactly("ana10@correo.test");
        assertThat(c.getValue().getText()).contains("Bs 1250,50").contains("R-00000009");
        assertEquals(Estado.ENVIADO, servicio.mias(10L).get(0).estado());
    }

    @Test
    void ventaEnTiendaNoAvisaYStockBajoAvisaAlPersonal() {
        assertEquals(0, servicio.procesar(pago(10, "TIENDA")));
        Evento stock = Evento.nuevo("stock.bajo", "inventario", null, "stock", 4,
                Map.of("nombre", "Ryzen 5 5600", "disponible", 1, "minimo", 3));
        assertEquals(2, servicio.procesar(stock));
        assertThat(servicio.mias(2L).get(0).mensaje()).contains("Ryzen 5 5600").contains("1 unidades");
        assertEquals(0, servicio.procesar(Evento.nuevo("producto.creado", "catalogo", 1L, "producto", 1, Map.of())));
    }

    @Test
    void conElCorreoCaidoReintentaAbreElCircuitoYNoPierdeNada() {
        doThrow(new MailSendException("SMTP caído")).when(correo).send(any(SimpleMailMessage.class));
        for (int i = 0; i < 6; i++) {
            servicio.procesar(pago(100 + i, "LINEA"));
        }
        for (int i = 0; i < 6; i++) {
            servicio.enviarPendientes();
        }
        // 3 intentos por llamada; tras 5 llamadas fallidas el circuito se abre y deja de intentar.
        verify(correo, times(15)).send(any(SimpleMailMessage.class));
        assertEquals(CircuitBreaker.State.OPEN, servicio.circuito().getState());
        assertEquals(6, servicio.resumen().pendientes(), "los avisos esperan en la tabla, no se pierden");

        // El servidor vuelve: el circuito se cierra y salen todos los pendientes.
        reset(correo);
        servicio.circuito().transitionToHalfOpenState();
        servicio.enviarPendientes();
        verify(correo, atLeast(6)).send(any(SimpleMailMessage.class));
        assertEquals(0, servicio.resumen().pendientes());
        assertEquals(CircuitBreaker.State.CLOSED, servicio.circuito().getState());
    }

    @Test
    void sinCorreoDelUsuarioQuedaFallida() {
        when(usuarios.contacto(55L)).thenReturn(Optional.empty());
        servicio.procesar(pago(55, "LINEA"));
        servicio.enviarPendientes();
        verify(correo, never()).send(any(SimpleMailMessage.class));
        assertEquals(1, servicio.resumen().fallidas());
    }
}
