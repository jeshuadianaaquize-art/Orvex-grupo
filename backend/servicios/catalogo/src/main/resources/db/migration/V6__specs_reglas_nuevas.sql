-- Especificaciones que usan las reglas nuevas de la etapa 6 (RF-07): interfaces de almacenamiento de la placa,
-- frecuencia máxima de memoria y conectores PCIe de 8 pines de gráficas y fuentes.
-- DATOS DE DEMOSTRACIÓN aproximados: deben contrastarse con la ficha del fabricante antes de usarse con clientes.
-- Se identifica cada producto por su SKU (no por id), así la migración no depende del orden de inserción.

INSERT INTO especificacion_tecnica (id_producto, nombre, valor)
SELECT p.id_producto, d.nombre, d.valor
FROM producto p
JOIN (
    SELECT 'MB-B550M' AS sku, 'interfaces_almacenamiento' AS nombre, 'M.2,SATA' AS valor UNION ALL
    SELECT 'MB-B550M', 'frecuencia_max_mhz', '4400' UNION ALL
    SELECT 'MB-B650M', 'interfaces_almacenamiento', 'M.2,SATA' UNION ALL
    SELECT 'MB-B650M', 'frecuencia_max_mhz', '6400' UNION ALL
    SELECT 'MB-B660M', 'interfaces_almacenamiento', 'M.2,SATA' UNION ALL
    SELECT 'MB-B660M', 'frecuencia_max_mhz', '3200' UNION ALL
    SELECT 'MB-B550-ATX', 'interfaces_almacenamiento', 'M.2,SATA' UNION ALL
    SELECT 'MB-B550-ATX', 'frecuencia_max_mhz', '4400' UNION ALL
    SELECT 'MB-B760M-D5', 'interfaces_almacenamiento', 'M.2,SATA' UNION ALL
    SELECT 'MB-B760M-D5', 'frecuencia_max_mhz', '5600' UNION ALL
    SELECT 'GPU-RTX4060', 'conectores_pcie', '1' UNION ALL
    SELECT 'GPU-RX7600', 'conectores_pcie', '1' UNION ALL
    SELECT 'GPU-RTX4070', 'conectores_pcie', '2' UNION ALL
    SELECT 'GPU-RX7800XT', 'conectores_pcie', '2' UNION ALL
    SELECT 'FTE-450W', 'conectores_pcie', '1' UNION ALL
    SELECT 'FTE-650W', 'conectores_pcie', '2' UNION ALL
    SELECT 'FTE-750W', 'conectores_pcie', '2'
) d ON d.sku = p.sku
-- Si el administrador ya cargó el dato, se respeta su valor.
WHERE NOT EXISTS (
    SELECT 1 FROM especificacion_tecnica e WHERE e.id_producto = p.id_producto AND e.nombre = d.nombre
);
