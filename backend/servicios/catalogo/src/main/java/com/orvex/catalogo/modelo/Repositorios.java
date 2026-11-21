package com.orvex.catalogo.modelo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorios del esquema orvex_catalogo. */
public final class Repositorios {

    private Repositorios() {
    }

    public interface CategoriaRepository extends JpaRepository<CategoriaEntidad, Short> {
        Optional<CategoriaEntidad> findByNombre(String nombre);
    }

    public interface ProductoRepository extends JpaRepository<Producto, Long> {

        /** Trae las especificaciones en la misma consulta (evita N+1 al armar el catálogo). */
        @EntityGraph(attributePaths = "especificaciones")
        List<Producto> findByEstadoIn(Collection<EstadoProducto> estados);

        @EntityGraph(attributePaths = "especificaciones")
        List<Producto> findByIdInAndEstadoIn(Collection<Long> ids, Collection<EstadoProducto> estados);

        @EntityGraph(attributePaths = "especificaciones")
        Optional<Producto> findWithEspecificacionesById(Long id);

        @EntityGraph(attributePaths = "especificaciones")
        List<Producto> findAllByOrderByNombreAsc();

        boolean existsBySkuIgnoreCase(String sku);

        boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    }

    public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
        List<Proveedor> findAllByOrderByNombreAsc();

        boolean existsByNombreIgnoreCase(String nombre);

        boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
    }
}
