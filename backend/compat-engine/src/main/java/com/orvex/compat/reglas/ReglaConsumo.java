package com.orvex.compat.reglas;

import com.orvex.compat.Categoria;
import com.orvex.compat.Componente;
import com.orvex.compat.Regla;
import com.orvex.compat.ResultadoRegla;
import com.orvex.compat.Severidad;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Consumo estimado = TDP(CPU) + TDP(GPU) + base (placa, RAM, almacenamiento, ventiladores). */
public final class ReglaConsumo implements Regla {

    public static final int BASE_W = 100;
    public static final double MARGEN = 1.3;
    private static final Pattern GRAMATICA = Pattern.compile(
            "^\\s*consumo\\s*\\(\\s*base\\s*=\\s*(\\d{1,4})\\s*\\)\\s*\\*\\s*(\\d(?:\\.\\d{1,3})?)\\s*<=\\s*b\\.potencia_w\\s*$",
            Pattern.CASE_INSENSITIVE);

    private final Long id;
    private final String nombre;
    private final int baseW;
    private final double margen;

    public ReglaConsumo(Long id, String nombre, int baseW, double margen) {
        if (baseW < 0 || margen < 1.0 || margen > 3.0) {
            throw new IllegalArgumentException("La base debe ser 0 o más y el margen debe estar entre 1.0 y 3.0.");
        }
        this.id = id;
        this.nombre = nombre;
        this.baseW = baseW;
        this.margen = margen;
    }

    public static boolean esConsumo(String condicion) {
        return condicion != null && condicion.trim().toLowerCase().startsWith("consumo");
    }

    public static ReglaConsumo parsear(Long id, String nombre, String condicion) {
        Matcher m = GRAMATICA.matcher(condicion == null ? "" : condicion);
        if (!m.matches()) {
            throw new IllegalArgumentException("La regla de consumo se escribe así: consumo(base=100) * 1.30 <= b.potencia_w");
        }
        return new ReglaConsumo(id, nombre, Integer.parseInt(m.group(1)), new BigDecimal(m.group(2)).doubleValue());
    }

    /** Devuelve 0 si no hay procesador ni gráfica con TDP conocido. */
    public int estimarW(List<Componente> armado) {
        int total = 0;
        boolean hayConsumidor = false;
        for (Categoria c : new Categoria[]{Categoria.PROCESADOR, Categoria.TARJETA_GRAFICA}) {
            Optional<Componente> comp = Regla.buscar(armado, c);
            if (comp.isPresent() && comp.get().specs().get("tdp_w") instanceof Number n) {
                total += n.intValue();
                hayConsumidor = true;
            }
        }
        return hayConsumidor ? total + baseW : 0;
    }

    @Override
    public Optional<ResultadoRegla> evaluar(List<Componente> armado) {
        Optional<Componente> fuente = Regla.buscar(armado, Categoria.FUENTE);
        Optional<Componente> cpu = Regla.buscar(armado, Categoria.PROCESADOR);
        Optional<Componente> gpu = Regla.buscar(armado, Categoria.TARJETA_GRAFICA);
        if (fuente.isEmpty() || (cpu.isEmpty() && gpu.isEmpty())) {
            return Optional.empty();
        }
        boolean faltaTdp = (cpu.isPresent() && !(cpu.get().specs().get("tdp_w") instanceof Number))
                || (gpu.isPresent() && !(gpu.get().specs().get("tdp_w") instanceof Number));
        if (faltaTdp || !(fuente.get().specs().get("potencia_w") instanceof Number potencia)) {
            return resultado(Severidad.ADVERTENCIA, "No se pudo verificar: faltan el consumo (TDP) o la potencia de la fuente.");
        }
        int estimado = estimarW(armado);
        int recomendado = (int) Math.ceil(estimado * margen);
        int w = potencia.intValue();
        int pct = (int) Math.round((margen - 1) * 100);
        if (w >= recomendado) {
            return resultado(Severidad.COMPATIBLE, "Consumo estimado " + estimado + " W; la fuente de " + w
                    + " W cubre con el margen de seguridad del " + pct + " %.");
        }
        if (w >= estimado) {
            return resultado(Severidad.ADVERTENCIA, "Consumo estimado " + estimado + " W; la fuente de " + w
                    + " W alcanza pero con poco margen (se recomiendan " + recomendado + " W).");
        }
        return resultado(Severidad.INCOMPATIBLE, "Consumo estimado " + estimado + " W; la fuente de " + w + " W no alcanza.");
    }

    private Optional<ResultadoRegla> resultado(Severidad s, String m) {
        return Optional.of(new ResultadoRegla(id, nombre, s, m));
    }
}
