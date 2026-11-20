package com.orvex.compatibilidad.servicio;

import com.orvex.compat.Componente;
import com.orvex.compat.ReporteCompatibilidad;
import com.orvex.compatibilidad.CatalogoCliente;
import com.orvex.compatibilidad.CatalogoCliente.ProductoCatalogo;
import com.orvex.compat.Especificaciones;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

/** Verifica un armado con las reglas vigentes de la base de datos (HU-07, RF-07, RF-08). */
@Service
public class VerificacionService {

    public record Resultado(ReporteCompatibilidad reporte, BigDecimal precioTotal, List<ProductoCatalogo> componentes) {
    }

    private final CatalogoCliente catalogo;
    private final ReglasService reglas;

    public VerificacionService(CatalogoCliente catalogo, ReglasService reglas) {
        this.catalogo = catalogo;
        this.reglas = reglas;
    }

    public Resultado verificar(List<Long> productoIds) {
        var idsUnicos = new LinkedHashSet<>(productoIds);
        List<ProductoCatalogo> productos = catalogo.productosPublicados(idsUnicos).stream()
                .sorted(Comparator.comparing(ProductoCatalogo::categoria)).toList();
        if (productos.size() != idsUnicos.size()) {
            throw new IllegalArgumentException("Algún producto del armado no existe o no está disponible.");
        }
        long categoriasDistintas = productos.stream().map(ProductoCatalogo::categoria).distinct().count();
        if (categoriasDistintas != productos.size()) {
            throw new IllegalArgumentException("El armado solo admite un componente por categoría.");
        }
        List<Componente> armado = productos.stream()
                .map(p -> new Componente(String.valueOf(p.id()), p.categoria(), p.nombre(), Especificaciones.tipar(p.specs())))
                .toList();
        BigDecimal total = productos.stream().map(ProductoCatalogo::precio).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Resultado(reglas.motor().verificar(armado), total, productos);
    }
}
