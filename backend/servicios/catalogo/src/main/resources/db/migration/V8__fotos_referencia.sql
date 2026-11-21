-- Fotos reales de referencia para los productos de demostración.
-- Son fotos de Wikimedia Commons con licencia libre (CC0, CC BY, CC BY-SA, FAL, GFDL o dominio público), guardadas en
-- la web (frontend/public/img/productos). Algunas muestran el modelo exacto y otras un componente del mismo tipo, por eso
-- la web las marca como "Foto de referencia". Autor y licencia de cada una: página /creditos (frontend/lib/creditos-fotos.ts).
-- Solo se asignan a productos SIN foto: no reemplaza las que haya subido el administrador.

UPDATE producto p
JOIN (
    SELECT 'CPU-R5-5600' AS sku, '/img/productos/cpu-r5-5600.jpg' AS url UNION ALL
    SELECT 'CPU-R5-7600', '/img/productos/cpu-r5-7600.jpg' UNION ALL
    SELECT 'CPU-I5-12400F', '/img/productos/cpu-i5-12400f.jpg' UNION ALL
    SELECT 'CPU-R7-5700X', '/img/productos/cpu-r7-5700x.jpg' UNION ALL
    SELECT 'CPU-R7-7700', '/img/productos/cpu-r7-7700.jpg' UNION ALL
    SELECT 'CPU-I7-13700K', '/img/productos/cpu-i7-13700k.jpg' UNION ALL
    SELECT 'GPU-RTX4060', '/img/productos/gpu-rtx4060.jpg' UNION ALL
    SELECT 'GPU-RTX4070', '/img/productos/gpu-rtx4070.jpg' UNION ALL
    SELECT 'GPU-RX7600', '/img/productos/gpu-rx7600.jpg' UNION ALL
    SELECT 'GPU-RX7800XT', '/img/productos/gpu-rx7800xt.jpg' UNION ALL
    SELECT 'MB-B550-ATX', '/img/productos/mb-b550-atx.jpg' UNION ALL
    SELECT 'MB-B550M', '/img/productos/mb-b550m.jpg' UNION ALL
    SELECT 'MB-B650M', '/img/productos/mb-b650m.jpg' UNION ALL
    SELECT 'MB-B660M', '/img/productos/mb-b660m.jpg' UNION ALL
    SELECT 'MB-B760M-D5', '/img/productos/mb-b760m-d5.jpg' UNION ALL
    SELECT 'RAM-D4-16', '/img/productos/ram-d4-16.jpg' UNION ALL
    SELECT 'RAM-D5-16', '/img/productos/ram-d5-16.jpg' UNION ALL
    SELECT 'RAM-D4-32', '/img/productos/ram-d4-32.jpg' UNION ALL
    SELECT 'RAM-D5-32', '/img/productos/ram-d5-32.jpg' UNION ALL
    SELECT 'SSD-NVME-1T', '/img/productos/ssd-nvme-1t.jpg' UNION ALL
    SELECT 'SSD-NVME-2T', '/img/productos/ssd-nvme-2t.jpg' UNION ALL
    SELECT 'HDD-2T', '/img/productos/hdd-2t.jpg' UNION ALL
    SELECT 'FTE-450W', '/img/productos/fte-450w.jpg' UNION ALL
    SELECT 'FTE-650W', '/img/productos/fte-650w.jpg' UNION ALL
    SELECT 'FTE-750W', '/img/productos/fte-750w.jpg' UNION ALL
    SELECT 'GAB-ATX-MID', '/img/productos/gab-atx-mid.jpg' UNION ALL
    SELECT 'GAB-MATX-MINI', '/img/productos/gab-matx-mini.jpg' UNION ALL
    SELECT 'GAB-ATX-VIDRIO', '/img/productos/gab-atx-vidrio.jpg' UNION ALL
    SELECT 'REF-TORRE-155', '/img/productos/ref-torre-155.jpg' UNION ALL
    SELECT 'REF-PERFIL-47', '/img/productos/ref-perfil-47.jpg'
) f ON f.sku = p.sku
SET p.foto_url = f.url
WHERE p.foto_url IS NULL;
