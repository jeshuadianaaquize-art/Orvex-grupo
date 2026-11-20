-- Esquema orvex_optimizacion según el diccionario de datos (tablas 17 y 18).
-- id_configuracion (orvex_compatibilidad) e id_producto (orvex_catalogo) no tienen FK: viven en otros esquemas (D3).

CREATE TABLE analisis_optimizacion (
    id_analisis      INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    id_configuracion INT UNSIGNED     NOT NULL,
    puntaje          TINYINT UNSIGNED NOT NULL,
    cuello_botella   VARCHAR(100)     NULL,
    recomendaciones  TEXT             NULL,
    fecha            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_analisis),
    CONSTRAINT uq_analisis_configuracion UNIQUE (id_configuracion),
    CONSTRAINT ck_analisis_puntaje CHECK (puntaje BETWEEN 0 AND 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE alternativa_sugerida (
    id_analisis INT UNSIGNED NOT NULL,
    id_producto INT UNSIGNED NOT NULL,
    PRIMARY KEY (id_analisis, id_producto),
    CONSTRAINT fk_alternativa_analisis FOREIGN KEY (id_analisis) REFERENCES analisis_optimizacion (id_analisis)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
