-- Especificaciones que usa el optimizador (etapa 7, M05): núcleos e hilos del procesador, memoria de video,
-- módulos de cada kit de RAM (doble canal) e índice de rendimiento relativo de procesadores y gráficas.
-- DATOS DE DEMOSTRACIÓN: el "rendimiento" es un índice relativo 1-100 entre los productos de este catálogo,
-- estimado a partir de comparativas públicas. El informe no fija una fuente: el equipo debe revisarlo
-- (docs/optimizacion.md) y el administrador lo puede corregir en la ficha de cada producto.

INSERT INTO especificacion_tecnica (id_producto, nombre, valor)
SELECT p.id_producto, d.nombre, d.valor
FROM producto p
JOIN (
    SELECT 'CPU-I5-12400F' AS sku, 'nucleos' AS nombre, '6' AS valor UNION ALL
    SELECT 'CPU-I5-12400F', 'hilos', '12' UNION ALL
    SELECT 'CPU-I5-12400F', 'rendimiento', '60' UNION ALL
    SELECT 'CPU-R5-5600', 'nucleos', '6' UNION ALL
    SELECT 'CPU-R5-5600', 'hilos', '12' UNION ALL
    SELECT 'CPU-R5-5600', 'rendimiento', '58' UNION ALL
    SELECT 'CPU-R5-7600', 'nucleos', '6' UNION ALL
    SELECT 'CPU-R5-7600', 'hilos', '12' UNION ALL
    SELECT 'CPU-R5-7600', 'rendimiento', '72' UNION ALL
    SELECT 'CPU-R7-5700X', 'nucleos', '8' UNION ALL
    SELECT 'CPU-R7-5700X', 'hilos', '16' UNION ALL
    SELECT 'CPU-R7-5700X', 'rendimiento', '66' UNION ALL
    SELECT 'CPU-R7-7700', 'nucleos', '8' UNION ALL
    SELECT 'CPU-R7-7700', 'hilos', '16' UNION ALL
    SELECT 'CPU-R7-7700', 'rendimiento', '82' UNION ALL
    SELECT 'CPU-I7-13700K', 'nucleos', '16' UNION ALL
    SELECT 'CPU-I7-13700K', 'hilos', '24' UNION ALL
    SELECT 'CPU-I7-13700K', 'rendimiento', '95' UNION ALL
    SELECT 'GPU-RX7600', 'vram_gb', '8' UNION ALL
    SELECT 'GPU-RX7600', 'rendimiento', '55' UNION ALL
    SELECT 'GPU-RTX4060', 'vram_gb', '8' UNION ALL
    SELECT 'GPU-RTX4060', 'rendimiento', '58' UNION ALL
    SELECT 'GPU-RTX4070', 'vram_gb', '12' UNION ALL
    SELECT 'GPU-RTX4070', 'rendimiento', '85' UNION ALL
    SELECT 'GPU-RX7800XT', 'vram_gb', '16' UNION ALL
    SELECT 'GPU-RX7800XT', 'rendimiento', '88' UNION ALL
    SELECT 'RAM-D4-16', 'modulos', '1' UNION ALL
    SELECT 'RAM-D5-16', 'modulos', '2' UNION ALL
    SELECT 'RAM-D4-32', 'modulos', '2' UNION ALL
    SELECT 'RAM-D5-32', 'modulos', '2'
) d ON d.sku = p.sku
-- Si el administrador ya cargó el dato, se respeta su valor.
WHERE NOT EXISTS (
    SELECT 1 FROM especificacion_tecnica e WHERE e.id_producto = p.id_producto AND e.nombre = d.nombre
);
