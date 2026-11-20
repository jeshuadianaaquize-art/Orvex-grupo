package com.orvex.usuarios.modelo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositorios del esquema orvex_usuarios. */
public final class Repositorios {

    private Repositorios() {
    }

    public interface RolRepository extends JpaRepository<RolUsuario, Short> {
        Optional<RolUsuario> findByNombre(String nombre);
    }

    public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

        Optional<Usuario> findByCorreoIgnoreCase(String correo);

        boolean existsByCorreoIgnoreCase(String correo);

        long countByRol_NombreAndEstado(String rol, EstadoUsuario estado);

        /** Personal activo con ciertos roles (destinatarios de las alertas de stock). */
        java.util.List<Usuario> findByRol_NombreInAndEstado(java.util.Collection<String> roles, EstadoUsuario estado);

        /** Búsqueda del panel de administración: texto en nombre o correo, y filtros opcionales. */
        @Query("""
                SELECT u FROM Usuario u
                WHERE (:texto IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                                      OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :texto, '%')))
                  AND (:rol IS NULL OR u.rol.nombre = :rol)
                  AND (:estado IS NULL OR u.estado = :estado)
                """)
        Page<Usuario> buscar(@Param("texto") String texto, @Param("rol") String rol,
                             @Param("estado") EstadoUsuario estado, Pageable pagina);
    }

    public interface DireccionRepository extends JpaRepository<Direccion, Long> {

        List<Direccion> findByUsuarioIdOrderByIdAsc(Long usuarioId);

        Optional<Direccion> findByIdAndUsuarioId(Long id, Long usuarioId);

        long countByUsuarioId(Long usuarioId);
    }
}
