package com.orvex.catalogo.web;

import com.orvex.catalogo.Categoria;
import com.orvex.catalogo.servicio.CatalogoService;
import com.orvex.catalogo.servicio.CatalogoService.Filtros;
import com.orvex.catalogo.servicio.CatalogoService.Orden;
import com.orvex.catalogo.servicio.FotoService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo público (HU-04): solo productos publicados. */
@RestController
public class ProductoController {

    static final int MAX_IDS = 50;

    private final CatalogoService catalogo;
    private final FotoService fotos;

    public ProductoController(CatalogoService catalogo, FotoService fotos) {
        this.catalogo = catalogo;
        this.fotos = fotos;
    }

    /** Sin parámetros: todo el catálogo publicado */
    @GetMapping("/api/productos")
    public List<Dtos.ProductoDto> listar(@RequestParam(required = false) Categoria categoria,
                                         @RequestParam(required = false) List<Long> ids) {
        if (ids != null) {
            if (ids.isEmpty() || ids.size() > MAX_IDS) {
                throw new IllegalArgumentException("Se pueden consultar entre 1 y " + MAX_IDS + " productos a la vez.");
            }
            return catalogo.porIds(ids);
        }
        return catalogo.listar(categoria);
    }

    /** Búsqueda con filtros (RF-04), paginación y facetas para armar los filtros de la tienda. */
    @GetMapping("/api/productos/buscar")
    public Dtos.Busqueda buscar(@RequestParam(required = false) String q,
                                @RequestParam(required = false) Categoria categoria,
                                @RequestParam(required = false) List<String> marca,
                                @RequestParam(required = false) BigDecimal precioMin,
                                @RequestParam(required = false) BigDecimal precioMax,
                                @RequestParam(required = false) String socket,
                                @RequestParam(required = false) String tipoRam,
                                @RequestParam(defaultValue = "false") boolean disponible,
                                @RequestParam(defaultValue = "PRECIO_ASC") Orden orden,
                                @RequestParam(defaultValue = "0") int pagina,
                                @RequestParam(defaultValue = "12") int tamano) {
        var filtros = new Filtros(q, categoria, marca == null ? null : new HashSet<>(marca), precioMin, precioMax,
                vacio(socket), vacio(tipoRam), disponible);
        return catalogo.buscar(filtros, orden, pagina, tamano);
    }

    @GetMapping("/api/productos/{id}")
    public Dtos.Detalle detalle(@PathVariable Long id) {
        return catalogo.detalle(id);
    }

    @GetMapping("/api/categorias")
    public List<Dtos.CategoriaDto> categorias() {
        return catalogo.categorias();
    }

    /** Fotos públicas */
    @GetMapping("/api/catalogo/fotos/{archivo}")
    public ResponseEntity<Resource> foto(@PathVariable String archivo) {
        return fotos.archivo(archivo)
                .map(p -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable())
                        .contentType(MediaTypeFactory.getMediaType(p.getFileName().toString()).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .header("X-Content-Type-Options", "nosniff")
                        .<Resource>body(new FileSystemResource(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    private static String vacio(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
