package com.orvex.compat.reglas;

import com.orvex.compat.Categoria;
import com.orvex.compat.Componente;
import com.orvex.compat.Regla;
import com.orvex.compat.ResultadoRegla;
import com.orvex.compat.Severidad;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Regla entre dos categorías definida por una condición de texto */
public final class ReglaCondicion implements Regla {

    public enum Operador {
        IGUAL("="), MENOR_IGUAL("<="), MAYOR_IGUAL(">="), MENOR("<"), MAYOR(">"), CONTIENE("contiene"), EN("en");

        final String simbolo;

        Operador(String simbolo) {
            this.simbolo = simbolo;
        }

        static Operador de(String s) {
            for (Operador o : values()) {
                if (o.simbolo.equalsIgnoreCase(s)) {
                    return o;
                }
            }
            throw new IllegalArgumentException("Operador desconocido: " + s);
        }

        boolean numerico() {
            return this == MENOR_IGUAL || this == MAYOR_IGUAL || this == MENOR || this == MAYOR;
        }
    }

    private static final Pattern GRAMATICA = Pattern.compile(
            "^\\s*a\\.([a-z][a-z0-9_]{0,39})\\s*(<=|>=|=|<|>|contiene|en)\\s*"
                    + "(?:b\\.([a-z][a-z0-9_]{0,39})|(\\d+(?:\\.\\d+)?))\\s*(\\[advertencia])?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private final Long id;
    private final String nombre;
    private final Categoria catA;
    private final Categoria catB;
    private final String claveA;
    private final Operador operador;
    private final String claveB;
    private final BigDecimal constante;
    private final Severidad siFalla;

    private ReglaCondicion(Long id, String nombre, Categoria catA, Categoria catB, String claveA, Operador operador,
                           String claveB, BigDecimal constante, Severidad siFalla) {
        this.id = id;
        this.nombre = nombre;
        this.catA = catA;
        this.catB = catB;
        this.claveA = claveA;
        this.operador = operador;
        this.claveB = claveB;
        this.constante = constante;
        this.siFalla = siFalla;
    }

    /** @throws IllegalArgumentException con un mensaje legible si la condición no respeta la gramática. */
    public static ReglaCondicion parsear(Long id, String nombre, Categoria catA, Categoria catB, String condicion) {
        if (condicion == null) {
            throw new IllegalArgumentException("Falta la condición.");
        }
        Matcher m = GRAMATICA.matcher(condicion);
        if (!m.matches()) {
            throw new IllegalArgumentException("La condición no se entiende. Usa por ejemplo: a.socket = b.socket, "
                    + "a.largo_mm <= b.max_largo_gpu_mm o a.formatos contiene b.formato.");
        }
        Operador op = Operador.de(m.group(2));
        BigDecimal constante = m.group(4) == null ? null : new BigDecimal(m.group(4));
        if (constante != null && !op.numerico() && op != Operador.IGUAL) {
            throw new IllegalArgumentException("Con un número solo se pueden usar =, <=, >=, < o >.");
        }
        Severidad siFalla = m.group(5) != null ? Severidad.ADVERTENCIA : Severidad.INCOMPATIBLE;
        return new ReglaCondicion(id, nombre, catA, catB, m.group(1).toLowerCase(Locale.ROOT), op,
                m.group(3) == null ? null : m.group(3).toLowerCase(Locale.ROOT), constante, siFalla);
    }

    @Override
    public Optional<ResultadoRegla> evaluar(List<Componente> armado) {
        Optional<Componente> a = Regla.buscar(armado, catA);
        Optional<Componente> b = constante == null ? Regla.buscar(armado, catB) : a;
        if (a.isEmpty() || b.isEmpty()) {
            return Optional.empty(); // la regla no aplica: falta una de las categorías
        }
        Object va = a.get().specs().get(claveA);
        Object vb = constante != null ? constante : b.get().specs().get(claveB);
        if (va == null || vb == null) {
            String quien = va == null ? a.get().nombre() : b.get().nombre();
            String que = Etiquetas.de(va == null ? claveA : claveB).nombre();
            return resultado(Severidad.ADVERTENCIA, "No se pudo verificar: falta el dato \"" + que + "\" de " + quien + ".");
        }
        Boolean cumple = cumple(va, vb);
        if (cumple == null) {
            return resultado(Severidad.ADVERTENCIA, "No se pudo verificar: las especificaciones de " + a.get().nombre()
                    + " o de " + b.get().nombre() + " tienen un formato inválido.");
        }
        return resultado(cumple ? Severidad.COMPATIBLE : siFalla, mensaje(cumple, a.get(), b.get(), va, vb));
    }

    /** null = los datos no permiten comparar (por ejemplo, texto donde se esperaba un número). */
    private Boolean cumple(Object va, Object vb) {
        return switch (operador) {
            case IGUAL -> va instanceof Number x && vb instanceof Number y
                    ? Double.compare(x.doubleValue(), y.doubleValue()) == 0
                    : texto(va).equalsIgnoreCase(texto(vb));
            case MENOR_IGUAL, MAYOR_IGUAL, MENOR, MAYOR -> {
                if (!(va instanceof Number x) || !(vb instanceof Number y)) {
                    yield null;
                }
                int c = Double.compare(x.doubleValue(), y.doubleValue());
                yield switch (operador) {
                    case MENOR_IGUAL -> c <= 0;
                    case MAYOR_IGUAL -> c >= 0;
                    case MENOR -> c < 0;
                    default -> c > 0;
                };
            }
            case CONTIENE -> lista(va).stream().anyMatch(x -> x.equalsIgnoreCase(texto(vb)));
            case EN -> lista(vb).stream().anyMatch(x -> x.equalsIgnoreCase(texto(va)));
        };
    }

    private String mensaje(boolean ok, Componente a, Componente b, Object va, Object vb) {
        String ea = Etiquetas.de(claveA).nombre();
        String da = conUnidad(va, claveA);
        String db = constante != null ? conUnidad(vb, claveA) : conUnidad(vb, claveB);
        String limite = constante != null ? "el valor exigido (" + db + ")" : b.nombre() + " (" + db + ")";
        return switch (operador) {
            case IGUAL -> ok
                    ? a.nombre() + " y " + b.nombre() + " coinciden en " + ea + ": " + da + "."
                    : "No coinciden en " + ea + ": " + a.nombre() + " usa " + da + " y " + b.nombre() + " usa " + db + ".";
            // Sin artículo ("El/La ...") para no equivocar el género con etiquetas que define el administrador.
            case MENOR_IGUAL, MENOR -> ok
                    ? a.nombre() + " (" + ea + ": " + da + ") está dentro de lo que admite " + limite + "."
                    : a.nombre() + " (" + ea + ": " + da + ") supera lo que admite " + limite + ".";
            case MAYOR_IGUAL, MAYOR -> ok
                    ? a.nombre() + " (" + ea + ": " + da + ") cumple lo que necesita " + limite + "."
                    : a.nombre() + " (" + ea + ": " + da + ") no alcanza lo que necesita " + limite + ".";
            case CONTIENE -> ok
                    ? a.nombre() + " admite " + db + ", que es lo que usa " + b.nombre() + "."
                    : a.nombre() + " admite " + da + ", pero " + b.nombre() + " usa " + db + ".";
            case EN -> ok
                    ? b.nombre() + " admite " + da + ", que es lo que usa " + a.nombre() + "."
                    : a.nombre() + " usa " + da + ", pero " + b.nombre() + " solo admite " + db + ".";
        };
    }

    private Optional<ResultadoRegla> resultado(Severidad s, String mensaje) {
        return Optional.of(new ResultadoRegla(id, nombre, s, mensaje));
    }

    private static List<String> lista(Object v) {
        if (v instanceof Collection<?> c) {
            return c.stream().map(ReglaCondicion::texto).toList();
        }
        return List.of(texto(v).split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String texto(Object v) {
        if (v instanceof Double d && d == Math.floor(d) && !d.isInfinite()) {
            return String.valueOf(d.longValue());
        }
        if (v instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(v).trim();
    }

    private static String conUnidad(Object v, String clave) {
        String unidad = Etiquetas.de(clave).unidad();
        String t = v instanceof Collection<?> c ? c.stream().map(ReglaCondicion::texto).collect(Collectors.joining(", ")) : texto(v);
        return unidad == null ? t : t + " " + unidad;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
}
