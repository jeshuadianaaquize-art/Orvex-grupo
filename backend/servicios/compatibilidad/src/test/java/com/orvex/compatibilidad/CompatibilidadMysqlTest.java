package com.orvex.compatibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.orvex.common.web.ApiException;
import com.orvex.compat.Categoria;
import com.orvex.compat.Severidad;
import com.orvex.compatibilidad.CatalogoCliente.ProductoCatalogo;
import com.orvex.compatibilidad.modelo.Modelo.UsoPrevisto;
import com.orvex.compatibilidad.servicio.ConfiguracionService;
import com.orvex.compatibilidad.servicio.ReglasService;
import com.orvex.compatibilidad.servicio.VerificacionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Reglas cargadas desde la tabla y configuraciones guardadas, contra MySQL 8 real */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class CompatibilidadMysqlTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @MockBean
    CatalogoCliente catalogo;

    @Autowired
    ReglasService reglas;
    @Autowired
    VerificacionService verificacion;
    @Autowired
    ConfiguracionService configuraciones;
    @Autowired
    JdbcTemplate jdbc;

    static ProductoCatalogo p(long id, Categoria c, String precio, Map<String, String> specs) {
        return new ProductoCatalogo(id, "SKU-" + id, "Producto " + id, null, new BigDecimal(precio), null, c, specs, 5, true);
    }

    static final ProductoCatalogo CPU = p(1, Categoria.PROCESADOR, "1100", Map.of("socket", "AM4", "tdp_w", "65"));
    static final ProductoCatalogo PLACA = p(4, Categoria.PLACA_MADRE, "780", Map.of("socket", "AM4", "tipo_ram", "DDR4",
            "factor_forma", "mATX", "interfaces_almacenamiento", "M.2,SATA", "frecuencia_max_mhz", "4400"));
    static final ProductoCatalogo HDD = p(28, Categoria.ALMACENAMIENTO, "380", Map.of("interfaz", "SATA"));

    @BeforeEach
    void catalogo() {
        when(catalogo.productosPublicados(anyCollection())).thenAnswer(i -> {
            var ids = (java.util.Collection<?>) i.getArgument(0);
            return List.of(CPU, PLACA, HDD).stream().filter(x -> ids.contains(x.id())).toList();
        });
    }

    @Test
    void lasDiezReglasInicialesSeCarganDesdeLaTabla() {
        assertThat(reglas.listar()).hasSize(10)
                .extracting(ReglasService.ReglaDto::nombre)
                .contains("Interfaz de almacenamiento", "Conectores de la fuente", "Frecuencia de la memoria");
        var r = verificacion.verificar(List.of(1L, 4L, 28L));
        assertEquals(Severidad.COMPATIBLE, r.reporte().global(), r.reporte().resultados().toString());
        assertThat(r.reporte().resultados()).allMatch(x -> x.reglaId() != null);
    }

    @Test
    void editarUnaReglaCambiaElResultadoSinTocarElCodigo() {
        var socket = reglas.listar().stream().filter(x -> x.nombre().equals("Interfaz de almacenamiento")).findFirst().orElseThrow();
        // El administrador decide que los discos solo pueden ser M.2: el HDD SATA deja de ser compatible.
        reglas.actualizar(1L, socket.id(), new ReglasService.Entrada(Categoria.ALMACENAMIENTO, Categoria.PLACA_MADRE,
                socket.nombre(), "a.interfaz = b.solo_m2"));
        var r = verificacion.verificar(List.of(4L, 28L));
        assertEquals(Severidad.ADVERTENCIA, r.reporte().global()); // la placa no tiene "solo_m2": no se puede verificar
        reglas.actualizar(1L, socket.id(), new ReglasService.Entrada(Categoria.ALMACENAMIENTO, Categoria.PLACA_MADRE,
                socket.nombre(), "a.interfaz en b.interfaces_almacenamiento"));
        assertEquals(Severidad.COMPATIBLE, verificacion.verificar(List.of(4L, 28L)).reporte().global());
    }

    @Test
    void unaCondicionMalEscritaNoSeGuarda() {
        var e = assertThrows(ApiException.class, () -> reglas.crear(1L, new ReglasService.Entrada(Categoria.RAM,
                Categoria.PLACA_MADRE, "Mala", "a.tipo_ram == b.tipo_ram")));
        assertThat(e.getCampos()).containsKey("condicion");
        assertThat(reglas.listar()).noneMatch(r -> r.nombre().equals("Mala"));
    }

    @Test
    void guardarAbrirYEliminarUnaConfiguracionPropia() {
        var d = configuraciones.guardar(7L, "PC de estudio", UsoPrevisto.OFICINA, List.of(1L, 4L, 28L));
        Long id = d.configuracion().id();
        assertEquals(0, new BigDecimal("2260").compareTo(d.configuracion().precioTotal()));
        assertEquals(Severidad.COMPATIBLE, d.configuracion().estado());
        Integer resultados = jdbc.queryForObject("SELECT COUNT(*) FROM resultado_compatibilidad WHERE id_configuracion = ?",
                Integer.class, id);
        assertEquals(2, resultados); // socket (CPU-placa) e interfaz (disco-placa): las únicas que aplican

        assertThat(configuraciones.listar(7L)).extracting(ConfiguracionService.Resumen::nombre).contains("PC de estudio");
        assertEquals(List.of(1L, 4L, 28L), configuraciones.abrir(7L, id).productoIds());

        // Otro usuario no la ve ni la puede borrar (404, no 403: no revela que existe).
        var ajeno = assertThrows(ApiException.class, () -> configuraciones.abrir(8L, id));
        assertEquals(404, ajeno.getEstado().value());
        assertThrows(ApiException.class, () -> configuraciones.eliminar(8L, id));

        configuraciones.eliminar(7L, id);
        assertThat(configuraciones.listar(7L)).noneMatch(c -> c.id().equals(id));
        Integer huerfanos = jdbc.queryForObject("SELECT COUNT(*) FROM item_configuracion WHERE id_configuracion = ?", Integer.class, id);
        assertEquals(0, huerfanos);
    }

    @Test
    void unaReglaUsadaEnConfiguracionesNoSeElimina() {
        configuraciones.guardar(9L, "Para probar", null, List.of(1L, 4L));
        var socket = reglas.listar().stream().filter(x -> x.nombre().equals("Socket del procesador")).findFirst().orElseThrow();
        var e = assertThrows(ApiException.class, () -> reglas.eliminar(1L, socket.id()));
        assertEquals(409, e.getEstado().value());
    }
}
