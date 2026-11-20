package com.orvex.compatibilidad.modelo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorios del esquema orvex_compatibilidad. */
public final class Repositorios {

    private Repositorios() {
    }

    public interface ReglaRepository extends JpaRepository<ReglaCompatibilidad, Long> {
        List<ReglaCompatibilidad> findAllByOrderByIdAsc();
    }

    public interface ConfiguracionRepository extends JpaRepository<ConfiguracionPc, Long> {

        @EntityGraph(attributePaths = {"items"})
        List<ConfiguracionPc> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

        @EntityGraph(attributePaths = {"items"})
        Optional<ConfiguracionPc> findByIdAndUsuarioId(Long id, Long usuarioId);

        long countByUsuarioId(Long usuarioId);

        @Query("SELECT COUNT(r) FROM ResultadoCompatibilidad r WHERE r.reglaId = :regla")
        long resultadosDeRegla(@Param("regla") Long reglaId);
    }
}
