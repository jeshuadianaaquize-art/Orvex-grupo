package com.orvex.optimizacion.modelo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalisisRepository extends JpaRepository<AnalisisOptimizacion, Long> {
    Optional<AnalisisOptimizacion> findByConfiguracionId(Long configuracionId);
}
