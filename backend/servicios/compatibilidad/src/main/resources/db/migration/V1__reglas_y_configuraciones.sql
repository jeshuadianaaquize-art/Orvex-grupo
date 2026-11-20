-- Tablas 13 a 16 del diccionario (armador de PC). id_categoria_*, id_usuario e id_producto pertenecen a los
-- servicios de catálogo y usuarios: se guardan sin FK entre esquemas (decisión D3, docs/arquitectura.md).
-- Los id de categoría coinciden con la tabla categoria del catálogo (1 Procesador … 8 Refrigeración).

CREATE TABLE regla_compatibilidad (
    id_regla       SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_categoria_a TINYINT UNSIGNED  NOT NULL,
    id_categoria_b TINYINT UNSIGNED  NOT NULL,
    nombre         VARCHAR(80)       NOT NULL,
    condicion      VARCHAR(255)      NOT NULL,
    PRIMARY KEY (id_regla),
    CONSTRAINT chk_regla_categorias CHECK (id_categoria_a BETWEEN 1 AND 8 AND id_categoria_b BETWEEN 1 AND 8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE configuracion_pc (
    id_configuracion   INT UNSIGNED      NOT NULL AUTO_INCREMENT,
    id_usuario         INT UNSIGNED      NOT NULL,
    nombre             VARCHAR(80)       NOT NULL,
    uso_previsto       ENUM('juegos','edicion','oficina') NULL,
    consumo_estimado_w SMALLINT UNSIGNED NULL,
    precio_total       DECIMAL(10,2)     NOT NULL DEFAULT 0.00,
    fecha_creacion     DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_configuracion),
    KEY idx_configuracion_usuario (id_usuario, fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE item_configuracion (
    id_item_config   INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    id_configuracion INT UNSIGNED     NOT NULL,
    id_producto      INT UNSIGNED     NOT NULL,
    cantidad         TINYINT UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY (id_item_config),
    KEY idx_item_configuracion (id_configuracion),
    CONSTRAINT fk_item_configuracion FOREIGN KEY (id_configuracion) REFERENCES configuracion_pc (id_configuracion)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_item_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resultado_compatibilidad (
    id_resultado     INT UNSIGNED      NOT NULL AUTO_INCREMENT,
    id_configuracion INT UNSIGNED      NOT NULL,
    id_regla         SMALLINT UNSIGNED NOT NULL,
    resultado        ENUM('compatible','advertencia','incompatible') NOT NULL,
    mensaje          VARCHAR(255)      NULL,
    PRIMARY KEY (id_resultado),
    KEY idx_resultado_configuracion (id_configuracion, resultado),
    CONSTRAINT fk_resultado_configuracion FOREIGN KEY (id_configuracion) REFERENCES configuracion_pc (id_configuracion)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_resultado_regla FOREIGN KEY (id_regla) REFERENCES regla_compatibilidad (id_regla)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Reglas iniciales (RF-07, RF-08). Gramática en docs/compatibilidad.md. Umbrales propuestos, no medidos (decisión D8).
-- 1 Procesador · 2 Placa madre · 3 Memoria RAM · 4 Tarjeta gráfica · 5 Almacenamiento · 6 Fuente · 7 Gabinete · 8 Refrigeración
INSERT INTO regla_compatibilidad (id_categoria_a, id_categoria_b, nombre, condicion) VALUES
(1, 2, 'Socket del procesador',        'a.socket = b.socket'),
(3, 2, 'Tipo de memoria RAM',          'a.tipo_ram = b.tipo_ram'),
(3, 2, 'Frecuencia de la memoria',     'a.frecuencia_mhz <= b.frecuencia_max_mhz [advertencia]'),
(7, 2, 'Formato de placa y gabinete',  'a.factores_forma contiene b.factor_forma'),
(4, 7, 'Largo de la tarjeta gráfica',  'a.largo_mm <= b.max_largo_gpu_mm'),
(8, 7, 'Altura del cooler',            'a.altura_mm <= b.max_altura_cooler_mm'),
(8, 1, 'Cooler y socket',              'a.sockets contiene b.socket'),
(5, 2, 'Interfaz de almacenamiento',   'a.interfaz en b.interfaces_almacenamiento'),
(4, 6, 'Conectores de la fuente',      'a.conectores_pcie <= b.conectores_pcie'),
(1, 6, 'Consumo y fuente de poder',    'consumo(base=100) * 1.30 <= b.potencia_w');
