package com.orvex.catalogo.servicio;

import com.orvex.eventos.PublicadorEventos;
import com.orvex.catalogo.Categoria;
import com.orvex.catalogo.modelo.EstadoProducto;
import com.orvex.catalogo.modelo.Producto;
import com.orvex.catalogo.modelo.Repositorios.CategoriaRepository;
import com.orvex.catalogo.modelo.Repositorios.ProductoRepository;
import com.orvex.catalogo.web.Dtos;
import com.orvex.common.web.ApiException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión de productos y especificaciones por el administrador (HU-05, RF-05). */
@Service
public class AdminProductoService {

    /** Cambios de precio y de estado (RF-26). Hasta M07 quedan en este log; luego se publican como evento. */
    private static final Logger auditoria = LoggerFactory.getLogger("AUDITORIA");
    private static final java.util.regex.Pattern NUMERO = java.util.regex.Pattern.compile("^\\d+(\\.\\d+)?$");

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final PublicadorEventos eventos;

    public AdminProductoService(ProductoRepository productos, CategoriaRepository categorias, PublicadorEventos eventos) {
        this.productos = productos;
        this.categorias = categorias;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<Dtos.ProductoAdminDto> listar() {
        return productos.findAllByOrderByNombreAsc().stream().map(AdminProductoService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public Dtos.ProductoAdminDto obtener(Long id) {
        return aDto(buscar(id));
    }

    /** Todo producto nuevo nace en borrador: no se ve en la tienda hasta publicarlo. */
    @Transactional
    public Dtos.ProductoAdminDto crear(Long adminId, Dtos.ProductoEntrada e) {
        String sku = e.sku().trim().toUpperCase();
        if (productos.existsBySkuIgnoreCase(sku)) {
            throw ApiException.conflicto("sku", "Ya existe un producto con este SKU.");
        }
        Map<String, String> specs = validarEspecificaciones(e.categoria(), e.especificaciones());
        Producto p = new Producto(categoria(e.categoria()), sku, e.nombre().trim(), limpio(e.marca()), e.precio());
        p.reemplazarEspecificaciones(specs);
        p = productos.save(p);
        auditoria.info("accion=producto_creado usuario_responsable={} producto={} sku={} precio={}", adminId, p.getId(), sku, e.precio());
        eventos.publicar("producto.creado", adminId, "producto", p.getId(), Map.of("sku", sku, "nombre", p.getNombre(), "precio", e.precio()));
        return aDto(p);
    }

    @Transactional
    public Dtos.ProductoAdminDto actualizar(Long adminId, Long id, Dtos.ProductoEntrada e) {
        Producto p = buscar(id);
        String sku = e.sku().trim().toUpperCase();
        if (productos.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw ApiException.conflicto("sku", "Ya existe otro producto con este SKU.");
        }
        Map<String, String> specs = validarEspecificaciones(e.categoria(), e.especificaciones());
        if (p.getEstado() == EstadoProducto.PUBLICADO) {
            exigirRequeridas(e.categoria(), specs); // un producto publicado no puede quedar incompleto
        }
        BigDecimal precioAnterior = p.getPrecio();
        p.cambiarCategoria(categoria(e.categoria()));
        p.actualizar(sku, e.nombre().trim(), limpio(e.marca()), e.precio());
        p.reemplazarEspecificaciones(specs);
        if (precioAnterior.compareTo(e.precio()) != 0) {
            auditoria.info("accion=cambio_precio usuario_responsable={} producto={} anterior={} nuevo={}",
                    adminId, id, precioAnterior, e.precio());
            eventos.publicar("producto.precio_cambiado", adminId, "producto", id,
                    Map.of("nombre", p.getNombre(), "anterior", precioAnterior, "nuevo", e.precio()));
        }
        return aDto(p);
    }

    @Transactional
    public Dtos.ProductoAdminDto cambiarEstado(Long adminId, Long id, String estadoTexto) {
        Producto p = buscar(id);
        EstadoProducto destino;
        try {
            destino = EstadoProducto.valueOf(estadoTexto.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estado desconocido: " + estadoTexto);
        }
        if (p.getEstado() == destino) {
            return aDto(p);
        }
        if (!p.getEstado().puedePasarA(destino)) {
            throw new ApiException(HttpStatus.CONFLICT, "Un producto " + p.getEstado().name().toLowerCase()
                    + " no puede pasar a " + destino.name().toLowerCase() + ".");
        }
        if (destino == EstadoProducto.PUBLICADO) {
            exigirRequeridas(p.getCategoria().codigo(), p.specs());
        }
        auditoria.info("accion=cambio_estado_producto usuario_responsable={} producto={} anterior={} nuevo={}",
                adminId, id, p.getEstado(), destino);
        eventos.publicar("producto.estado_cambiado", adminId, "producto", id,
                Map.of("nombre", p.getNombre(), "anterior", p.getEstado().name(), "nuevo", destino.name()));
        p.cambiarEstado(destino);
        return aDto(p);
    }

    @Transactional
    public Dtos.ProductoAdminDto cambiarFoto(Long id, String url) {
        Producto p = buscar(id);
        p.cambiarFoto(url);
        return aDto(p);
    }

    @Transactional(readOnly = true)
    public String fotoActual(Long id) {
        return buscar(id).getFotoUrl();
    }

    public List<Dtos.CategoriaSpecsDto> catalogoDeEspecificaciones() {
        return Arrays.stream(Categoria.values())
                .map(c -> new Dtos.CategoriaSpecsDto(c, c.nombreBd(), c.especificaciones().stream().map(Dtos.SpecDto::de).toList()))
                .toList();
    }

    // validación

    /** Normaliza y valida las especificaciones según su tipo conocido */
    static Map<String, String> validarEspecificaciones(Categoria cat, List<Dtos.EspecificacionEntrada> entrada) {
        Map<String, String> r = new LinkedHashMap<>();
        Map<String, String> errores = new LinkedHashMap<>();
        if (entrada != null) {
            for (var e : entrada) {
                String nombre = e.nombre().trim();
                String valor = e.valor().trim();
                if (r.containsKey(nombre)) {
                    errores.put("especificaciones." + nombre, "está repetida");
                    continue;
                }
                var spec = cat.spec(nombre);
                if (spec.isPresent() && spec.get().tipo() == Categoria.Tipo.NUMERO) {
                    valor = valor.replace(',', '.');
                    if (!NUMERO.matcher(valor).matches()) {
                        errores.put("especificaciones." + nombre, spec.get().etiqueta() + " debe ser un número");
                        continue;
                    }
                } else if (spec.isPresent() && spec.get().tipo() == Categoria.Tipo.LISTA) {
                    valor = Arrays.stream(valor.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                            .distinct().collect(Collectors.joining(","));
                    if (valor.isEmpty()) {
                        errores.put("especificaciones." + nombre, spec.get().etiqueta() + " necesita al menos un valor");
                        continue;
                    }
                }
                if (valor.length() > 80) {
                    errores.put("especificaciones." + nombre, "máximo 80 caracteres");
                    continue;
                }
                r.put(nombre, valor);
            }
        }
        if (!errores.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Revisa las especificaciones marcadas.", errores);
        }
        return r;
    }

    static List<String> faltantes(Categoria cat, Map<String, String> specs) {
        return cat.especificaciones().stream().filter(Categoria.Spec::requerida)
                .filter(s -> !specs.containsKey(s.clave())).map(Categoria.Spec::etiqueta).toList();
    }

    private static void exigirRequeridas(Categoria cat, Map<String, String> specs) {
        List<String> f = faltantes(cat, specs);
        if (!f.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Para publicarlo faltan especificaciones que usa la verificación de compatibilidad: " + String.join(", ", f) + ".");
        }
    }

    private Producto buscar(Long id) {
        return productos.findWithEspecificacionesById(id).orElseThrow(() -> ApiException.noEncontrado("El producto no existe."));
    }

    private com.orvex.catalogo.modelo.CategoriaEntidad categoria(Categoria c) {
        return categorias.findByNombre(c.nombreBd()).orElseThrow();
    }

    private static String limpio(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static Dtos.ProductoAdminDto aDto(Producto p) {
        var specs = p.specs();
        return new Dtos.ProductoAdminDto(p.getId(), p.getSku(), p.getNombre(), p.getMarca(), p.getPrecio(), p.getFotoUrl(),
                p.getCategoria().codigo(), p.getEstado().name(), specs, faltantes(p.getCategoria().codigo(), specs));
    }
}
