package com.orvex.optimizacion.servicio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orvex.common.web.ApiException;
import com.orvex.compat.Categoria;
import com.orvex.compat.Componente;
import com.orvex.compat.Especificaciones;
import com.orvex.compat.MotorCompatibilidad;
import com.orvex.compat.Regla;
import com.orvex.compat.ReporteCompatibilidad;
import com.orvex.compat.ResultadoRegla;
import com.orvex.compat.Severidad;
import com.orvex.compat.reglas.ReglasPorDefecto;
import com.orvex.optimizacion.analisis.Analizador;
import com.orvex.optimizacion.analisis.Analizador.Hallazgo;
import com.orvex.optimizacion.analisis.Analizador.Pieza;
import com.orvex.optimizacion.analisis.ParametrosOptimizacion;
import com.orvex.optimizacion.analisis.UsoPrevisto;
import com.orvex.optimizacion.clientes.CatalogoCliente;
import com.orvex.optimizacion.clientes.CatalogoCliente.Producto;
import com.orvex.optimizacion.clientes.CompatibilidadCliente;
import com.orvex.optimizacion.modelo.AnalisisOptimizacion;
import com.orvex.optimizacion.modelo.AnalisisRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Análisis de optimización (RF-09, HU-09) y alternativas (RF-10, HU-10). */
@Service
public class OptimizacionService {

    private static final Logger log = LoggerFactory.getLogger(OptimizacionService.class);
    private static final Duration VIGENCIA_REGLAS = Duration.ofSeconds(30);

    /** Un producto tal como lo necesita la pantalla de alternativas. */
    public record ProductoResumen(Long id, String nombre, String marca, BigDecimal precio, String fotoUrl, Integer stock) {
        static ProductoResumen de(Producto p) {
            return new ProductoResumen(p.id(), p.nombre(), p.marca(), p.precio(), p.fotoUrl(), p.stock());
        }
    }

    /** Reemplazar reemplaza (null si se agrega una pieza que falta) por producto. */
    public record Alternativa(Categoria categoria, ProductoResumen reemplaza, ProductoResumen producto, int puntajeNuevo,
                              BigDecimal diferenciaPrecio, String motivo) {
    }

    public record Analisis(int puntaje, String cuelloBotella, UsoPrevisto uso, Severidad compatibilidad, int consumoEstimadoW,
                           Double balance, List<Hallazgo> recomendaciones, List<String> fortalezas,
                           boolean buscoAlternativas, List<Alternativa> alternativas, int umbralAlternativas) {
    }

    /** Análisis guardado de una configuración (tablas analisis_optimizacion y alternativa_sugerida). */
    public record Guardado(Long configuracionId, int puntaje, String cuelloBotella, List<Hallazgo> recomendaciones,
                           List<Long> alternativas, LocalDateTime fecha) {
    }

    private record Reglas(MotorCompatibilidad motor, Map<Long, CompatibilidadCliente.Regla> porId, Instant hasta) {
    }

    private final CatalogoCliente catalogo;
    private final CompatibilidadCliente compatibilidad;
    private final AnalisisRepository analisis;
    private final ParametrosOptimizacion parametros;
    private final Analizador analizador;
    private final ObjectMapper json;
    private final Clock reloj;
    private volatile Reglas cache;

    public OptimizacionService(CatalogoCliente catalogo, CompatibilidadCliente compatibilidad, AnalisisRepository analisis,
                               ParametrosOptimizacion parametros, ObjectMapper json, Clock reloj) {
        this.catalogo = catalogo;
        this.compatibilidad = compatibilidad;
        this.analisis = analisis;
        this.parametros = parametros;
        this.analizador = new Analizador(parametros);
        this.json = json;
        this.reloj = reloj;
    }

    /** Análisis de un armado sin guardar (también para visitantes). */
    public Analisis analizar(List<Long> productoIds, UsoPrevisto uso) {
        List<Producto> todos = catalogo.publicados();
        Map<Long, Producto> porId = todos.stream().collect(Collectors.toMap(Producto::id, Function.identity()));
        var unicos = new LinkedHashSet<>(productoIds);
        List<Producto> armado = new ArrayList<>();
        for (Long id : unicos) {
            Producto p = porId.get(id);
            if (p == null) {
                throw new IllegalArgumentException("Algún producto del armado no existe o no está disponible.");
            }
            armado.add(p);
        }
        if (armado.stream().map(Producto::categoria).distinct().count() != armado.size()) {
            throw new IllegalArgumentException("El armado solo admite un componente por categoría.");
        }
        Reglas reglas = reglas();
        Evaluacion actual = evaluar(armado, uso, reglas);

        Set<Long> incompatibles = incompatibles(actual.reporte());
        boolean buscar = !incompatibles.isEmpty() || actual.resultado().puntaje() < parametros.umbralAlternativas();
        List<Alternativa> alternativas = buscar ? alternativas(armado, actual, uso, reglas, todos) : List.of();

        var r = actual.resultado();
        return new Analisis(r.puntaje(), r.cuelloBotella(), uso, actual.reporte().global(), actual.reporte().consumoEstimadoW(),
                r.balance(), r.recomendaciones(), r.fortalezas(), buscar, alternativas, parametros.umbralAlternativas());
    }

    /** Analiza una configuración guardada del usuario y guarda el resultado */
    @Transactional
    public Analisis analizarConfiguracion(Long usuarioId, Long configuracionId, UsoPrevisto usoIndicado) {
        var c = compatibilidad.configuracion(usuarioId, configuracionId);
        UsoPrevisto uso = c.uso() != null ? c.uso() : usoIndicado != null ? usoIndicado : UsoPrevisto.JUEGOS;
        Analisis a = analizar(c.productoIds(), uso);
        var fila = analisis.findByConfiguracionId(configuracionId).orElseGet(() -> new AnalisisOptimizacion(configuracionId));
        List<Long> sugeridas = a.alternativas().stream().map(x -> x.producto().id()).distinct().toList();
        fila.actualizar(a.puntaje(), a.cuelloBotella(), aJson(a.recomendaciones()), LocalDateTime.now(reloj), sugeridas);
        analisis.save(fila);
        return a;
    }

    @Transactional(readOnly = true)
    public Guardado guardado(Long usuarioId, Long configuracionId) {
        compatibilidad.configuracion(usuarioId, configuracionId); // 404 si no es suya
        var a = analisis.findByConfiguracionId(configuracionId)
                .orElseThrow(() -> ApiException.noEncontrado("Esta configuración todavía no tiene un análisis guardado."));
        return new Guardado(configuracionId, a.getPuntaje(), a.getCuelloBotella(), deJson(a.getRecomendaciones()),
                List.copyOf(a.getAlternativas()), a.getFecha());
    }

    // alternativas

    private record Evaluacion(ReporteCompatibilidad reporte, Analizador.Resultado resultado) {
    }

    private Evaluacion evaluar(List<Producto> armado, UsoPrevisto uso, Reglas reglas) {
        List<Componente> componentes = armado.stream()
                .map(p -> new Componente(String.valueOf(p.id()), p.categoria(), p.nombre(), Especificaciones.tipar(p.specs())))
                .toList();
        ReporteCompatibilidad reporte = reglas.motor().verificar(componentes);
        List<Pieza> piezas = armado.stream()
                .map(p -> new Pieza(p.id(), p.categoria(), p.nombre(), p.precio(), Especificaciones.tipar(p.specs())))
                .toList();
        return new Evaluacion(reporte, analizador.analizar(piezas, uso, reporte));
    }

    private static Set<Long> incompatibles(ReporteCompatibilidad r) {
        return r.resultados().stream().filter(x -> x.severidad() == Severidad.INCOMPATIBLE && x.reglaId() != null)
                .map(ResultadoRegla::reglaId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Prueba, una por una, las piezas con stock de las categorías señaladas */
    private List<Alternativa> alternativas(List<Producto> armado, Evaluacion actual, UsoPrevisto uso, Reglas reglas,
                                           List<Producto> todos) {
        Set<Long> malasAntes = incompatibles(actual.reporte());
        Set<Categoria> objetivo = EnumSet.noneOf(Categoria.class);
        for (Long regla : malasAntes) {
            var r = reglas.porId().get(regla);
            if (r != null) {
                objetivo.add(r.categoriaA());
                objetivo.add(r.categoriaB());
            }
        }
        actual.resultado().recomendaciones().stream()
                .filter(h -> h.puntos() > 0 && h.categoria() != null).forEach(h -> objetivo.add(h.categoria()));

        Map<Categoria, Producto> por = new EnumMap<>(Categoria.class);
        armado.forEach(p -> por.put(p.categoria(), p));
        Map<Long, String> nombres = new HashMap<>();
        reglas.porId().forEach((id, r) -> nombres.put(id, r.nombre()));

        List<Alternativa> salida = new ArrayList<>();
        for (Categoria cat : objetivo) {
            Producto actualCat = por.get(cat);
            List<Alternativa> deCategoria = new ArrayList<>();
            for (Producto candidato : todos) {
                if (candidato.categoria() != cat || (actualCat != null && candidato.id().equals(actualCat.id()))
                        || !Boolean.TRUE.equals(candidato.enStock()) || candidato.stock() == null || candidato.stock() <= 0) {
                    continue;
                }
                List<Producto> nuevo = new ArrayList<>(armado);
                nuevo.removeIf(p -> p.categoria() == cat);
                nuevo.add(candidato);
                Evaluacion e = evaluar(nuevo, uso, reglas);
                Set<Long> malasDespues = incompatibles(e.reporte());
                if (!malasAntes.containsAll(malasDespues)) {
                    continue; // agrega un problema nuevo: no pasa la compatibilidad
                }
                Set<Long> resueltas = new LinkedHashSet<>(malasAntes);
                resueltas.removeAll(malasDespues);
                int antes = actual.resultado().puntaje();
                int despues = e.resultado().puntaje();
                if (resueltas.isEmpty() && despues <= antes) {
                    continue;
                }
                String motivo = resueltas.isEmpty()
                        ? "Sube el puntaje de " + antes + " a " + despues + "."
                        : "Resuelve: " + resueltas.stream().map(id -> nombres.getOrDefault(id, "regla " + id))
                                .collect(Collectors.joining(", ")) + ". Puntaje " + antes + " → " + despues + ".";
                BigDecimal diferencia = candidato.precio().subtract(actualCat == null ? BigDecimal.ZERO : actualCat.precio());
                deCategoria.add(new Alternativa(cat, actualCat == null ? null : ProductoResumen.de(actualCat),
                        ProductoResumen.de(candidato), despues, diferencia, motivo));
            }
            deCategoria.sort(Comparator.comparingInt(Alternativa::puntajeNuevo).reversed()
                    .thenComparing(Alternativa::diferenciaPrecio));
            salida.addAll(deCategoria.stream().limit(parametros.alternativasPorCategoria()).toList());
        }
        return salida;
    }

    // reglas de compatibilidad

    private Reglas reglas() {
        Reglas c = cache;
        Instant ahora = reloj.instant();
        if (c != null && ahora.isBefore(c.hasta())) {
            return c;
        }
        List<Regla> motor = new ArrayList<>();
        Map<Long, CompatibilidadCliente.Regla> porId = new HashMap<>();
        for (var r : compatibilidad.reglas()) {
            try {
                motor.add(ReglasPorDefecto.crear(r.id(), r.nombre(), r.categoriaA(), r.categoriaB(), r.condicion()));
                porId.put(r.id(), r);
            } catch (IllegalArgumentException e) {
                log.warn("Regla {} ignorada: {}", r.id(), e.getMessage());
            }
        }
        c = new Reglas(new MotorCompatibilidad(motor), porId, ahora.plus(VIGENCIA_REGLAS));
        cache = c;
        return c;
    }

    // JSON de recomendaciones

    private String aJson(List<Hallazgo> h) {
        try {
            return json.writeValueAsString(h);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Hallazgo> deJson(String s) {
        if (s == null || s.isBlank()) {
            return List.of();
        }
        try {
            return json.readValue(s, new TypeReference<List<Hallazgo>>() { });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
