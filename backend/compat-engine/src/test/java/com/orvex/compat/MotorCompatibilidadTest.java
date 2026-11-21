package com.orvex.compat;

import static org.junit.jupiter.api.Assertions.*;

import com.orvex.compat.reglas.ReglaCondicion;
import com.orvex.compat.reglas.ReglaConsumo;
import com.orvex.compat.reglas.ReglasPorDefecto;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MotorCompatibilidadTest {

    private final MotorCompatibilidad motor = new MotorCompatibilidad(ReglasPorDefecto.crear());

    private static Componente c(String id, Categoria cat, Map<String, Object> specs) {
        return new Componente(id, cat, id, specs);
    }

    private static final Componente CPU_AM4 = c("Ryzen 5 5600", Categoria.PROCESADOR, Map.of("socket", "AM4", "tdp_w", 65));
    private static final Componente MB_AM4 = c("B550M", Categoria.PLACA_MADRE, Map.of("socket", "AM4", "tipo_ram", "DDR4",
            "factor_forma", "mATX", "frecuencia_max_mhz", 4400, "interfaces_almacenamiento", List.of("M.2", "SATA")));
    private static final Componente RAM_D4 = c("DDR4 16GB", Categoria.RAM, Map.of("tipo_ram", "DDR4", "frecuencia_mhz", 3200));
    private static final Componente GPU = c("RTX 4060", Categoria.TARJETA_GRAFICA, Map.of("largo_mm", 240, "tdp_w", 115, "conectores_pcie", 1));
    private static final Componente CASE_MID = c("ATX mid", Categoria.GABINETE,
            Map.of("factores_forma", List.of("ATX", "mATX"), "max_largo_gpu_mm", 330, "max_altura_cooler_mm", 160));
    private static final Componente PSU_650 = c("650W", Categoria.FUENTE, Map.of("potencia_w", 650, "conectores_pcie", 2));
    private static final Componente SSD_M2 = c("SSD NVMe", Categoria.ALMACENAMIENTO, Map.of("interfaz", "M.2"));

    private static Severidad de(ReporteCompatibilidad r, String regla) {
        return r.resultados().stream().filter(x -> x.regla().equals(regla)).findFirst().orElseThrow().severidad();
    }

    @Test
    void armadoCompatibleNoTieneIncompatibilidades() {
        var r = motor.verificar(List.of(CPU_AM4, MB_AM4, RAM_D4, GPU, CASE_MID, PSU_650, SSD_M2));
        assertEquals(Severidad.COMPATIBLE, r.global(), r.resultados().toString());
        assertEquals(280, r.consumoEstimadoW());
    }

    @Test
    void socketDistintoEsIncompatible() {
        var mbAm5 = c("B650M", Categoria.PLACA_MADRE, Map.of("socket", "AM5", "tipo_ram", "DDR5", "factor_forma", "mATX"));
        var r = motor.verificar(List.of(CPU_AM4, mbAm5));
        assertEquals(Severidad.INCOMPATIBLE, r.global());
        assertTrue(r.resultados().get(0).mensaje().contains("AM4") && r.resultados().get(0).mensaje().contains("AM5"));
    }

    @Test
    void ramDeTipoDistintoEsIncompatible() {
        var ramD5 = c("DDR5", Categoria.RAM, Map.of("tipo_ram", "DDR5", "frecuencia_mhz", 5200));
        assertEquals(Severidad.INCOMPATIBLE, de(motor.verificar(List.of(MB_AM4, ramD5)), "Tipo de memoria RAM"));
    }

    @Test
    void ramMasRapidaQueLaPlacaSoloAdvierte() {
        var rapida = c("DDR4 4800", Categoria.RAM, Map.of("tipo_ram", "DDR4", "frecuencia_mhz", 4800));
        var r = motor.verificar(List.of(MB_AM4, rapida));
        assertEquals(Severidad.ADVERTENCIA, de(r, "Frecuencia de la memoria"));
        assertEquals(Severidad.ADVERTENCIA, r.global());
    }

    @Test
    void gpuMasLargaQueElGabineteEsIncompatible() {
        var gpuLarga = c("GPU larga", Categoria.TARJETA_GRAFICA, Map.of("largo_mm", 340, "tdp_w", 200));
        var r = motor.verificar(List.of(gpuLarga, CASE_MID));
        assertEquals(Severidad.INCOMPATIBLE, r.global());
        assertTrue(r.resultados().get(0).mensaje().contains("340 mm"), r.resultados().get(0).mensaje());
    }

    @Test
    void discoSataEnPlacaSinSataEsIncompatible() {
        var soloM2 = c("Placa mini", Categoria.PLACA_MADRE, Map.of("socket", "AM4", "tipo_ram", "DDR4", "factor_forma", "ITX",
                "interfaces_almacenamiento", List.of("M.2")));
        var hdd = c("HDD 2TB", Categoria.ALMACENAMIENTO, Map.of("interfaz", "SATA"));
        assertEquals(Severidad.INCOMPATIBLE, de(motor.verificar(List.of(soloM2, hdd)), "Interfaz de almacenamiento"));
        assertEquals(Severidad.COMPATIBLE, de(motor.verificar(List.of(MB_AM4, hdd)), "Interfaz de almacenamiento"));
    }

    @Test
    void fuenteSinConectoresSuficientesEsIncompatible() {
        var gpu2 = c("RX 7800 XT", Categoria.TARJETA_GRAFICA, Map.of("largo_mm", 267, "tdp_w", 263, "conectores_pcie", 2));
        var psu1 = c("450W", Categoria.FUENTE, Map.of("potencia_w", 900, "conectores_pcie", 1));
        assertEquals(Severidad.INCOMPATIBLE, de(motor.verificar(List.of(gpu2, psu1)), "Conectores de la fuente"));
    }

    @Test
    void fuenteInsuficienteEsIncompatible() {
        var psu250 = c("250W", Categoria.FUENTE, Map.of("potencia_w", 250, "conectores_pcie", 1));
        assertEquals(Severidad.INCOMPATIBLE, motor.verificar(List.of(CPU_AM4, GPU, psu250)).global());
    }

    @Test
    void fuenteJustaDaAdvertencia() {
        var psu350 = c("350W", Categoria.FUENTE, Map.of("potencia_w", 350, "conectores_pcie", 1)); // estimado 280, recomendado 364
        assertEquals(Severidad.ADVERTENCIA, motor.verificar(List.of(CPU_AM4, GPU, psu350)).global());
    }

    @Test
    void elMargenYLaBaseDelConsumoSonConfigurables() {
        var estricto = new MotorCompatibilidad(List.of(ReglaConsumo.parsear(1L, "Consumo", "consumo(base=50) * 2.0 <= b.potencia_w")));
        var r = estricto.verificar(List.of(CPU_AM4, GPU, PSU_650)); // estimado 230 W, recomendado 460 W
        assertEquals(230, r.consumoEstimadoW());
        assertEquals(Severidad.COMPATIBLE, r.global());
        var psu400 = c("400W", Categoria.FUENTE, Map.of("potencia_w", 400));
        assertEquals(Severidad.ADVERTENCIA, estricto.verificar(List.of(CPU_AM4, GPU, psu400)).global());
    }

    @Test
    void especificacionFaltanteAdviertePeroNoDaPorCompatible() {
        var mbSinSocket = c("Placa sin datos", Categoria.PLACA_MADRE, Map.of("tipo_ram", "DDR4"));
        var r = motor.verificar(List.of(CPU_AM4, mbSinSocket));
        assertEquals(Severidad.ADVERTENCIA, r.global());
        assertTrue(r.resultados().get(0).mensaje().contains("socket"));
    }

    @Test
    void especificacionConFormatoInvalidoAdvierteEnVezDeFallar() {
        var gpuTexto = c("GPU con largo en texto", Categoria.TARJETA_GRAFICA, Map.of("largo_mm", "240", "tdp_w", 115));
        assertEquals(Severidad.ADVERTENCIA, motor.verificar(List.of(gpuTexto, CASE_MID)).global());
    }

    @Test
    void listasSeMuestranSinCorchetes() {
        var mbEatx = c("Placa E-ATX", Categoria.PLACA_MADRE, Map.of("socket", "AM4", "tipo_ram", "DDR4", "factor_forma", "EATX"));
        var r = motor.verificar(List.of(mbEatx, CASE_MID));
        var formato = r.resultados().stream().filter(x -> x.regla().startsWith("Formato")).findFirst().orElseThrow();
        assertEquals(Severidad.INCOMPATIBLE, formato.severidad());
        assertTrue(formato.mensaje().contains("ATX, mATX"), formato.mensaje());
    }

    @Test
    void sinParesVerificablesNoDiceCompatible() {
        assertEquals(Severidad.ADVERTENCIA, motor.verificar(List.of(RAM_D4, PSU_650)).global());
    }

    @Test
    void conMenosDeDosComponentesLanzaError() {
        assertThrows(IllegalArgumentException.class, () -> motor.verificar(List.of(CPU_AM4)));
    }

    @Test
    void condicionesMalEscritasSeRechazanConUnMensajeClaro() {
        for (String mala : List.of("a.socket == b.socket", "socket = socket", "a.socket = b.socket; DROP TABLE x",
                "a.largo_mm contiene 5", "System.exit(0)", "")) {
            assertThrows(IllegalArgumentException.class,
                    () -> ReglasPorDefecto.crear(1L, "x", Categoria.PROCESADOR, Categoria.PLACA_MADRE, mala), mala);
        }
        assertThrows(IllegalArgumentException.class, () -> ReglaConsumo.parsear(1L, "x", "consumo(base=100) * 9 <= b.potencia_w"));
    }

    @Test
    void condicionConConstante() {
        var r = ReglaCondicion.parsear(7L, "Cooler bajo", Categoria.REFRIGERACION, Categoria.REFRIGERACION, "a.altura_mm <= 60 [advertencia]");
        var alto = c("Cooler 155", Categoria.REFRIGERACION, Map.of("altura_mm", 155));
        var res = r.evaluar(List.of(alto)).orElseThrow();
        assertEquals(Severidad.ADVERTENCIA, res.severidad());
        assertEquals(7L, res.reglaId());
        assertTrue(res.mensaje().contains("60 mm"), res.mensaje());
    }
}
