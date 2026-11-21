-- Lleva el catálogo de la beta al modelo del diccionario de datos (tablas 6 a 9: categoria, producto,
-- especificacion_tecnica y proveedor). Conserva los id de producto, así los carritos guardados siguen valiendo.
-- La disponibilidad (antes producto.en_stock) pasa a la tabla stock del servicio de inventario.

CREATE TABLE categoria (
    id_categoria TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(40) NOT NULL,
    PRIMARY KEY (id_categoria),
    UNIQUE KEY uk_categoria_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Las 8 categorías del informe (RF-03). El orden fija sus id: el código del servicio los referencia por nombre.
INSERT INTO categoria (nombre) VALUES
('Procesador'), ('Placa madre'), ('Memoria RAM'), ('Tarjeta gráfica'),
('Almacenamiento'), ('Fuente de poder'), ('Gabinete'), ('Refrigeración');

CREATE TABLE proveedor (
    id_proveedor INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(80)  NOT NULL,
    contacto     VARCHAR(100) NULL,
    PRIMARY KEY (id_proveedor)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE especificacion_tecnica (
    id_especificacion INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_producto       INT UNSIGNED NOT NULL,
    nombre            VARCHAR(40) NOT NULL,
    valor             VARCHAR(80) NOT NULL,
    PRIMARY KEY (id_especificacion),
    UNIQUE KEY uk_especificacion_producto_nombre (id_producto, nombre),
    CONSTRAINT fk_especificacion_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 1) Especificaciones: cada clave del JSON pasa a una fila. Las listas se guardan separadas por coma.
INSERT INTO especificacion_tecnica (id_producto, nombre, valor)
SELECT p.id_producto,
       k.nombre,
       CASE
           WHEN JSON_TYPE(JSON_EXTRACT(p.specs, CONCAT('$."', k.nombre, '"'))) = 'ARRAY' THEN
               (SELECT GROUP_CONCAT(v.x ORDER BY v.i SEPARATOR ',')
                FROM JSON_TABLE(JSON_EXTRACT(p.specs, CONCAT('$."', k.nombre, '"')), '$[*]'
                                COLUMNS (i FOR ORDINALITY, x VARCHAR(80) PATH '$')) v)
           ELSE JSON_UNQUOTE(JSON_EXTRACT(p.specs, CONCAT('$."', k.nombre, '"')))
       END
FROM producto p
JOIN JSON_TABLE(JSON_KEYS(p.specs), '$[*]' COLUMNS (nombre VARCHAR(40) PATH '$')) k
WHERE p.specs IS NOT NULL;

-- 2) Categoría: del texto en mayúsculas al id de la tabla categoria.
ALTER TABLE producto ADD COLUMN id_categoria TINYINT UNSIGNED NULL AFTER id_producto;
UPDATE producto p JOIN categoria c ON c.nombre = CASE p.categoria
        WHEN 'PROCESADOR'      THEN 'Procesador'
        WHEN 'PLACA_MADRE'     THEN 'Placa madre'
        WHEN 'RAM'             THEN 'Memoria RAM'
        WHEN 'TARJETA_GRAFICA' THEN 'Tarjeta gráfica'
        WHEN 'ALMACENAMIENTO'  THEN 'Almacenamiento'
        WHEN 'FUENTE'          THEN 'Fuente de poder'
        WHEN 'GABINETE'        THEN 'Gabinete'
        WHEN 'REFRIGERACION'   THEN 'Refrigeración'
    END
SET p.id_categoria = c.id_categoria;

-- 3) Estado: de 'PUBLICADO' a 'publicado', como el dominio del diccionario.
ALTER TABLE producto MODIFY estado VARCHAR(20) NOT NULL;
UPDATE producto SET estado = LOWER(estado);

-- 4) Columnas finales según el diccionario.
ALTER TABLE producto
    DROP INDEX idx_producto_categoria_estado,
    DROP COLUMN categoria,
    DROP COLUMN en_stock,
    DROP COLUMN specs,
    MODIFY id_categoria TINYINT UNSIGNED NOT NULL,
    MODIFY estado ENUM('borrador','publicado','inactivo') NOT NULL DEFAULT 'borrador',
    ADD COLUMN foto_url VARCHAR(255) NULL AFTER precio,
    ADD KEY idx_producto_categoria_estado (id_categoria, estado),
    ADD CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT;
