package com.orvex.catalogo.servicio;

import com.orvex.catalogo.Categoria;
import com.orvex.catalogo.modelo.EstadoProducto;
import com.orvex.catalogo.modelo.Producto;
import com.orvex.catalogo.modelo.Repositorios.ProductoRepository;
import com.orvex.catalogo.servicio.InventarioCliente.Existencias;
import com.orvex.catalogo.web.Dtos;
import com.orvex.catalogo.web.Dtos.ProductoDto;
import com.orvex.common.web.ApiException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catálogo público (HU-04, RF-03, RF-04): listado, búsqueda con filtros, facetas y detalle. */
@Service
public class CatalogoService {

    public enum Orden { PRECIO_ASC, PRECIO_DESC, NOMBRE }

    public record Filtros(String texto, Categoria categoria, Set<String> marcas, BigDecimal precioMin,
                          BigDecimal precioMax, String socket, String tipoRam, boolean soloDisponibles) {
    }

    static final int MAX_POR_PAGINA = 48;
    static final int RELACIONADOS = 4;
    private static final Set<EstadoProducto> PUBLICADOS = EstadoProducto.visiblesEnTienda();

    private final ProductoRepository productos;
    private final InventarioCliente inventario;

    public CatalogoService(ProductoRepository productos, InventarioCliente inventario) {
        this.productos = productos;
        this.inventario = inventario;
    }

    /** Todo el catálogo publicado, o una categoría. Lo usan el armador, el inicio y el carrito. */
    @Transactional(readOnly = true)
    public List<ProductoDto> listar(Categoria categoria) {
        List<Producto> lista = productos.findByEstadoIn(PUBLICADOS).stream()
                .filter(p -> categoria == null || p.getCategoria().codigo() == categoria)
                .sorted(Comparator.comparing((Producto p) -> p.getCategoria().codigo()).thenComparing(Producto::getPrecio))
                .toList();
        return aDtos(lista);
    }

    /** Productos publicados por id. Lo usa el servicio de compatibilidad. */
    @Transactional(readOnly = true)
    public List<ProductoDto> porIds(Collection<Long> ids) {
        return aDtos(productos.findByIdInAndEstadoIn(ids, PUBLICADOS));
    }

    @Transactional(readOnly = true)
    public Dtos.Busqueda buscar(Filtros f, Orden orden, int pagina, int tamano) {
        int porPagina = Math.min(Math.max(tamano, 1), MAX_POR_PAGINA);
        List<Producto> publicados = productos.findByEstadoIn(PUBLICADOS);
        var existencias = inventario.existencias(publicados.stream().map(Producto::getId).toList());

        // Las facetas se calculan solo con la categoría elegida: así el usuario ve todas sus opciones.
        List<Producto> deCategoria = publicados.stream()
                .filter(p -> f.categoria() == null || p.getCategoria().codigo() == f.categoria()).toList();
        var facetas = new Dtos.Facetas(
                distintos(deCategoria, Producto::getMarca),
                distintos(deCategoria, p -> p.specs().get("socket")),
                distintos(deCategoria, p -> p.specs().get("tipo_ram")),
                deCategoria.stream().map(Producto::getPrecio).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));

        String texto = normalizar(f.texto());
        List<Producto> filtrados = deCategoria.stream()
                .filter(p -> texto.isEmpty() || Arrays.stream(texto.split("\\s+")).allMatch(t ->
                        normalizar(p.getNombre() + " " + Objects.toString(p.getMarca(), "") + " " + p.getSku()
                                + " " + p.getCategoria().getNombre()).contains(t)))
                .filter(p -> f.marcas() == null || f.marcas().isEmpty() || f.marcas().contains(p.getMarca()))
                .filter(p -> f.precioMin() == null || p.getPrecio().compareTo(f.precioMin()) >= 0)
                .filter(p -> f.precioMax() == null || p.getPrecio().compareTo(f.precioMax()) <= 0)
                .filter(p -> f.socket() == null || contieneValor(p.specs().get("socket"), f.socket())
                        || contieneValor(p.specs().get("sockets"), f.socket()))
                .filter(p -> f.tipoRam() == null || f.tipoRam().equalsIgnoreCase(p.specs().get("tipo_ram")))
                .filter(p -> !f.soloDisponibles() || existencias.isEmpty()
                        || existencias.get().getOrDefault(p.getId(), SIN_STOCK).vendibles() > 0)
                .sorted(comparador(orden))
                .toList();

        int totalPaginas = Math.max(1, (int) Math.ceil(filtrados.size() / (double) porPagina));
        int actual = Math.min(Math.max(pagina, 0), totalPaginas - 1);
        var pag = filtrados.subList(Math.min(actual * porPagina, filtrados.size()),
                Math.min((actual + 1) * porPagina, filtrados.size()));
        var mapa = existencias.orElse(null);
        return new Dtos.Busqueda(
                new Dtos.Pagina<>(pag.stream().map(p -> ProductoDto.de(p, mapa)).toList(), actual, totalPaginas, filtrados.size()),
                facetas, existencias.isPresent());
    }

    /** Ficha del producto con productos relacionados: misma categoría y precio más cercano. */
    @Transactional(readOnly = true)
    public Dtos.Detalle detalle(Long id) {
        Producto p = productos.findWithEspecificacionesById(id)
                .filter(x -> PUBLICADOS.contains(x.getEstado()))
                .orElseThrow(() -> ApiException.noEncontrado("El producto no existe o ya no está disponible."));
        List<Producto> relacionados = productos.findByEstadoIn(PUBLICADOS).stream()
                .filter(x -> !x.getId().equals(id) && x.getCategoria().codigo() == p.getCategoria().codigo())
                .sorted(Comparator.comparing((Producto x) -> x.getPrecio().subtract(p.getPrecio()).abs()))
                .limit(RELACIONADOS)
                .toList();
        var todos = new java.util.ArrayList<Long>();
        todos.add(p.getId());
        relacionados.forEach(x -> todos.add(x.getId()));
        var mapa = inventario.existencias(todos).orElse(null);
        return new Dtos.Detalle(ProductoDto.de(p, mapa), relacionados.stream().map(x -> ProductoDto.de(x, mapa)).toList());
    }

    @Transactional(readOnly = true)
    public List<Dtos.CategoriaDto> categorias() {
        Map<Categoria, List<Producto>> por = new EnumMap<>(Categoria.class);
        productos.findByEstadoIn(PUBLICADOS).forEach(p -> por.computeIfAbsent(p.getCategoria().codigo(), k -> new java.util.ArrayList<>()).add(p));
        return Arrays.stream(Categoria.values()).map(c -> {
            var l = por.getOrDefault(c, List.of());
            return new Dtos.CategoriaDto(c, c.nombreBd(), l.size(),
                    l.stream().map(Producto::getPrecio).min(Comparator.naturalOrder()).orElse(null));
        }).toList();
    }

    // utilidades

    private static final Existencias SIN_STOCK = new Existencias(0L, 0, 0, 0);

    private List<ProductoDto> aDtos(List<Producto> lista) {
        var mapa = inventario.existencias(lista.stream().map(Producto::getId).toList()).orElse(null);
        return lista.stream().map(p -> ProductoDto.de(p, mapa)).toList();
    }

    private static Comparator<Producto> comparador(Orden orden) {
        return switch (orden == null ? Orden.PRECIO_ASC : orden) {
            case PRECIO_DESC -> Comparator.comparing(Producto::getPrecio).reversed();
            case NOMBRE -> Comparator.comparing(p -> normalizar(p.getNombre()));
            case PRECIO_ASC -> Comparator.comparing(Producto::getPrecio);
        };
    }

    private static List<String> distintos(List<Producto> lista, Function<Producto, String> campo) {
        return lista.stream().map(campo).filter(Objects::nonNull)
                .flatMap(v -> Arrays.stream(v.split(","))).map(String::trim).filter(s -> !s.isEmpty())
                .distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    static boolean contieneValor(String guardado, String buscado) {
        return guardado != null && Arrays.stream(guardado.split(",")).anyMatch(v -> v.trim().equalsIgnoreCase(buscado));
    }

    /** Minúsculas y sin tildes: "gráfica" encuentra "grafica" y al revés. */
    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
