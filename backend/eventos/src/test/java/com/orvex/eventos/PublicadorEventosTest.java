package com.orvex.eventos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class PublicadorEventosTest {

    RabbitTemplate rabbit = mock(RabbitTemplate.class);

    @Test
    void deshabilitadoSoloRegistraEnElLog() {
        new PublicadorEventos(rabbit, "prueba", false).publicar("x.y", 1L, "venta", 3, Map.of());
        verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void publicaEnElExchangeConElTipoComoClave() {
        new PublicadorEventos(rabbit, "pedidos", true).publicar("pago.confirmado", null, "venta", 9, Map.of("total", 10));
        var c = ArgumentCaptor.forClass(Object.class);
        verify(rabbit, timeout(2000)).convertAndSend(eq(PublicadorEventos.EXCHANGE), eq("pago.confirmado"), c.capture());
        Evento e = (Evento) c.getValue();
        assertEquals("pedidos", e.servicio());
        assertEquals("9", e.entidadId());
        assertEquals(10L, e.numero("total"));
        assertNull(e.usuarioId());
    }

    @Test
    void conRabbitCaidoQuedaPendienteYSaleEnOrdenAlVolver() throws Exception {
        doThrow(new AmqpConnectException(new java.net.ConnectException("caído"))).when(rabbit)
                .convertAndSend(anyString(), anyString(), any(Object.class));
        PublicadorEventos p = new PublicadorEventos(rabbit, "inventario", true);
        p.publicar("a", 1L, "stock", 1, Map.of());
        p.publicar("b", 1L, "stock", 2, Map.of());
        for (int i = 0; i < 50 && p.pendientes() < 2; i++) {
            Thread.sleep(50);
        }
        assertEquals(2, p.pendientes());
        doNothing().when(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));
        p.reintentar();
        assertEquals(0, p.pendientes());
    }

    @Test
    void dentroDeUnaTransaccionEsperaAlCommit() {
        PublicadorEventos p = new PublicadorEventos(rabbit, "catalogo", true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            p.publicar("producto.precio_cambiado", 1L, "producto", 5, Map.of());
            verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        verify(rabbit, timeout(2000)).convertAndSend(eq(PublicadorEventos.EXCHANGE), eq("producto.precio_cambiado"), any(Object.class));
    }

    @Test
    void lecturaDeDatos() {
        Evento e = Evento.nuevo("t", "s", null, "e", null, Map.of("n", "42", "x", "no", "d", 2.5));
        assertEquals(42L, e.numero("n"));
        assertNull(e.numero("x"));
        assertEquals(2L, e.numero("d"));
        assertNull(e.entidadId());
        assertNull(e.texto("falta"));
    }
}
