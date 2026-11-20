package com.orvex.optimizacion.analisis;

import com.orvex.compat.Categoria;
import com.orvex.compat.ReporteCompatibilidad;
import com.orvex.compat.ResultadoRegla;
import com.orvex.compat.Severidad;
import com.orvex.optimizacion.analisis.ParametrosOptimizacion.Penalizaciones;
import com.orvex.optimizacion.analisis.ParametrosOptimizacion.Uso;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Puntaje de optimización 0-100 de un armado */
public final class Analizador {

    /** Un producto del armado con sus especificaciones ya interpretadas (números, listas, texto). */
    public record Pieza(Long id, Categoria categoria, String nombre, BigDecimal precio, Map<String, Object> specs) {
    }

    public enum Tipo { COMPATIBILIDAD, BALANCE, USO, NUCLEOS, RAM, DOBLE_CANAL, ALMACENAMIENTO, GASTO, DATOS }

    /** Algo que mejorar */
    public record Hallazgo(Tipo tipo, Categoria categoria, int puntos, String titulo, String explicacion) {
    }

    public record Resultado(int puntaje, String cuelloBotella, List<Hallazgo> recomendaciones, List<String> fortalezas,
                            Double balance) {
    }

    private final ParametrosOptimizacion p;

    public Analizador(ParametrosOptimizacion parametros) {
        this.p = parametros;
    }

    public Resultado analizar(List<Pieza> piezas, UsoPrevisto uso, ReporteCompatibilidad reporte) {
        Map<Categoria, Pieza> por = new EnumMap<>(Categoria.class);
        piezas.forEach(x -> por.put(x.categoria(), x));
        if (!por.containsKey(Categoria.PROCESADOR)) {
            throw new IllegalArgumentException("Elige al menos el procesador para analizar la optimización.");
        }
        Uso u = p.usos().de(uso);
        Penalizaciones pen = p.penalizaciones();
        List<Hallazgo> h = new ArrayList<>();
        List<String> bien = new ArrayList<>();

        compatibilidad(reporte, pen, h, bien);
        Pieza cpu = por.get(Categoria.PROCESADOR);
        Pieza gpu = por.get(Categoria.TARJETA_GRAFICA);
        Double balance = grafica(cpu, gpu, uso, u, pen, h, bien);
        nucleos(cpu, uso, u, pen, h, bien);
        memoria(por.get(Categoria.RAM), uso, u, pen, h, bien);
        disco(por.get(Categoria.ALMACENAMIENTO), pen, h, bien);
        faltantes(por, h);

        h.sort(Comparator.comparingInt(Hallazgo::puntos).reversed());
        int total = h.stream().mapToInt(Hallazgo::puntos).sum();
        int puntaje = Math.max(0, Math.min(100, 100 - total));
        if (reporte.global() == Severidad.INCOMPATIBLE) {
            puntaje = Math.min(puntaje, p.puntajeMaximoIncompatible());
        }
        String cuello = h.stream().filter(x -> x.puntos() > 0).findFirst().map(x -> recortar(x.titulo(), 100)).orElse(null);
        return new Resultado(puntaje, cuello, List.copyOf(h), List.copyOf(bien), balance);
    }

    // reglas

    private static void compatibilidad(ReporteCompatibilidad r, Penalizaciones pen, List<Hallazgo> h, List<String> bien) {
        List<ResultadoRegla> malas = r.resultados().stream().filter(x -> x.severidad() == Severidad.INCOMPATIBLE).toList();
        List<ResultadoRegla> avisos = r.resultados().stream().filter(x -> x.severidad() == Severidad.ADVERTENCIA).toList();
        int resta = Math.min(pen.maximoCompatibilidad(), malas.size() * pen.incompatible() + avisos.size() * pen.advertencia());
        if (!malas.isEmpty()) {
            h.add(new Hallazgo(Tipo.COMPATIBILIDAD, null, resta,
                    "Hay piezas incompatibles: " + malas.stream().map(ResultadoRegla::regla).collect(Collectors.joining(", ")),
                    "Esta PC no se puede comprar así. " + malas.get(0).mensaje()
                            + " Las alternativas de abajo resuelven el problema sin rehacer el armado."));
        } else if (!avisos.isEmpty()) {
            h.add(new Hallazgo(Tipo.COMPATIBILIDAD, null, resta,
                    avisos.size() == 1 ? "Una advertencia de compatibilidad" : avisos.size() + " advertencias de compatibilidad",
                    "Funciona, pero conviene revisarlo: " + avisos.stream().map(ResultadoRegla::mensaje).collect(Collectors.joining(" "))));
        } else {
            bien.add("Todas las piezas son compatibles entre sí.");
        }
    }

    private Double grafica(Pieza cpu, Pieza gpu, UsoPrevisto uso, Uso u, Penalizaciones pen, List<Hallazgo> h, List<String> bien) {
        if (gpu == null) {
            if (u.sinGrafica() > 0) {
                h.add(new Hallazgo(Tipo.USO, Categoria.TARJETA_GRAFICA, u.sinGrafica(), "Falta una tarjeta gráfica para " + uso.texto(),
                        "Para " + uso.texto() + " la gráfica integrada se queda corta. Agrega una tarjeta gráfica dedicada."));
            } else {
                bien.add("Para " + uso.texto() + " no hace falta una tarjeta gráfica dedicada.");
            }
            return null;
        }
        Optional<Double> rg = numero(gpu, "rendimiento");
        if (u.graficaMaximaUtil() > 0 && rg.isPresent() && rg.get() > u.graficaMaximaUtil()) {
            h.add(new Hallazgo(Tipo.GASTO, Categoria.TARJETA_GRAFICA, pen.gasto(), "Tarjeta gráfica de más para " + uso.texto(),
                    "Para " + uso.texto() + " no se aprovecha una gráfica de este nivel (" + entero(rg.get())
                            + " de rendimiento). Una más sencilla, o ninguna, baja el precio sin que notes la diferencia."));
        }
        if (u.sinGrafica() == 0) {
            return null; // el balance solo importa cuando el uso necesita gráfica
        }
        Optional<Double> rc = numero(cpu, "rendimiento");
        if (rg.isEmpty() || rc.isEmpty()) {
            h.add(new Hallazgo(Tipo.DATOS, null, 0, "No se pudo evaluar el balance procesador/gráfica",
                    "Falta el índice de rendimiento de " + (rc.isEmpty() ? cpu.nombre() : gpu.nombre())
                            + " en el catálogo. El puntaje no considera este punto."));
            return null;
        }
        double ratio = rg.get() / rc.get();
        String rango = "entre " + decimal(u.balanceMinimo()) + " y " + decimal(u.balanceMaximo());
        if (ratio > u.balanceMaximo()) {
            int resta = Math.min(pen.balance(), (int) Math.round((ratio / u.balanceMaximo() - 1) * 100));
            h.add(new Hallazgo(Tipo.BALANCE, Categoria.PROCESADOR, Math.max(resta, 1), "El procesador limita a la tarjeta gráfica",
                    "Para " + uso.texto() + " se propone que la gráfica rinda " + rango + " veces lo que rinde el procesador; aquí rinde "
                            + decimal(ratio) + " veces. " + cpu.nombre() + " no alcanzará a darle trabajo a " + gpu.nombre()
                            + " y parte de lo que pagas por la gráfica no se aprovecha."));
        } else if (ratio < u.balanceMinimo()) {
            int resta = Math.min(pen.balance(), (int) Math.round((1 - ratio / u.balanceMinimo()) * 100));
            h.add(new Hallazgo(Tipo.BALANCE, Categoria.TARJETA_GRAFICA, Math.max(resta, 1), "La tarjeta gráfica limita el rendimiento",
                    "Para " + uso.texto() + " se propone que la gráfica rinda " + rango + " veces lo que rinde el procesador; aquí rinde "
                            + decimal(ratio) + " veces. " + gpu.nombre() + " será el freno y el procesador queda sobrado."));
        } else {
            bien.add("Procesador y tarjeta gráfica equilibrados para " + uso.texto() + " (la gráfica rinde " + decimal(ratio)
                    + " veces el procesador).");
        }
        return Math.round(ratio * 100) / 100.0;
    }

    private static void nucleos(Pieza cpu, UsoPrevisto uso, Uso u, Penalizaciones pen, List<Hallazgo> h, List<String> bien) {
        Optional<Double> n = numero(cpu, "nucleos");
        if (n.isEmpty()) {
            return;
        }
        if (n.get() < u.nucleosMinimos()) {
            h.add(new Hallazgo(Tipo.NUCLEOS, Categoria.PROCESADOR, pen.nucleos(), "Pocos núcleos para " + uso.texto(),
                    cpu.nombre() + " tiene " + entero(n.get()) + " núcleos; para " + uso.texto() + " se proponen al menos "
                            + u.nucleosMinimos() + ". Con más núcleos, exportar y trabajar con varios programas toma menos tiempo."));
        } else {
            bien.add(entero(n.get()) + " núcleos: suficientes para " + uso.texto() + ".");
        }
    }

    private static void memoria(Pieza ram, UsoPrevisto uso, Uso u, Penalizaciones pen, List<Hallazgo> h, List<String> bien) {
        if (ram == null) {
            h.add(new Hallazgo(Tipo.RAM, Categoria.RAM, pen.ram(), "Falta la memoria RAM",
                    "Sin memoria RAM la PC no enciende. Para " + uso.texto() + " se proponen al menos " + u.ramMinimaGb() + " GB."));
            return;
        }
        Optional<Double> gb = numero(ram, "capacidad_gb");
        if (gb.isPresent()) {
            if (gb.get() < u.ramMinimaGb()) {
                double falta = 1 - gb.get() / u.ramMinimaGb();
                int resta = Math.min(pen.ram(), Math.max(pen.ram() / 2, (int) Math.round(pen.ram() * falta * 1.6)));
                h.add(new Hallazgo(Tipo.RAM, Categoria.RAM, resta, "Poca memoria RAM para " + uso.texto(),
                        entero(gb.get()) + " GB se quedan cortos para " + uso.texto() + ": se proponen al menos " + u.ramMinimaGb()
                                + " GB. Con poca memoria los programas se vuelven lentos al usar varios a la vez."));
            } else {
                bien.add(entero(gb.get()) + " GB de RAM: suficiente para " + uso.texto() + ".");
            }
        }
        Optional<Double> modulos = numero(ram, "modulos");
        if (modulos.isEmpty()) {
            h.add(new Hallazgo(Tipo.DATOS, null, 0, "No se pudo evaluar el doble canal",
                    "El catálogo no indica cuántos módulos trae " + ram.nombre() + "."));
        } else if (modulos.get() < 2) {
            h.add(new Hallazgo(Tipo.DOBLE_CANAL, Categoria.RAM, pen.dobleCanal(), "La memoria trabaja en un solo canal",
                    ram.nombre() + " es un solo módulo. Con dos módulos iguales la memoria trabaja en doble canal y "
                            + "transfiere datos más rápido, sobre todo en juegos y con gráficos integrados."));
        } else {
            bien.add("La memoria viene en " + entero(modulos.get()) + " módulos: trabaja en doble canal.");
        }
    }

    private static void disco(Pieza disco, Penalizaciones pen, List<Hallazgo> h, List<String> bien) {
        if (disco == null) {
            h.add(new Hallazgo(Tipo.ALMACENAMIENTO, Categoria.ALMACENAMIENTO, pen.sinSsd(), "Falta el almacenamiento",
                    "Sin disco no hay dónde instalar el sistema. Se propone un SSD M.2 como disco principal."));
            return;
        }
        Object interfaz = disco.specs().get("interfaz");
        if (interfaz != null && String.valueOf(interfaz).toUpperCase(Locale.ROOT).contains("M.2")) {
            bien.add("Disco M.2: el sistema y los programas cargan rápido.");
        } else if (interfaz != null) {
            h.add(new Hallazgo(Tipo.ALMACENAMIENTO, Categoria.ALMACENAMIENTO, pen.sinSsd(), "El disco principal es lento",
                    disco.nombre() + " usa SATA. Un SSD M.2 como disco principal hace que el sistema y los programas carguen "
                            + "varias veces más rápido; el disco SATA puede quedar para guardar archivos."));
        }
    }

    private static void faltantes(Map<Categoria, Pieza> por, List<Hallazgo> h) {
        List<String> sin = new ArrayList<>();
        for (Categoria c : List.of(Categoria.PLACA_MADRE, Categoria.FUENTE, Categoria.GABINETE)) {
            if (!por.containsKey(c)) {
                sin.add(nombre(c));
            }
        }
        if (!sin.isEmpty()) {
            h.add(new Hallazgo(Tipo.DATOS, null, 0, "Faltan piezas por elegir",
                    "Todavía falta: " + String.join(", ", sin) + ". El análisis considera solo las piezas elegidas."));
        }
    }

    // utilidades

    private static Optional<Double> numero(Pieza x, String clave) {
        Object v = x.specs().get(clave);
        return v instanceof Number n ? Optional.of(n.doubleValue()) : Optional.empty();
    }

    private static String nombre(Categoria c) {
        return switch (c) {
            case PLACA_MADRE -> "placa madre";
            case FUENTE -> "fuente de poder";
            case GABINETE -> "gabinete";
            default -> c.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    private static String entero(double d) {
        return String.valueOf(Math.round(d));
    }

    private static String decimal(double d) {
        return String.format(Locale.ROOT, "%.1f", d).replace('.', ',');
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
