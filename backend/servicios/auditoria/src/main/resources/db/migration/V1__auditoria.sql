-- Tabla 5 del diccionario (esquema orvex_auditoria). id_usuario vive en orvex_usuarios: sin FK entre esquemas (D3).
-- Extensión D50: id_evento (UNIQUE) para que una entrega repetida de la cola no duplique el registro, y
-- id_usuario admite NULL para las acciones del sistema (webhook del banco, vencimiento de reservas).
CREATE TABLE auditoria (
    id_auditoria     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario       INT UNSIGNED    NULL,
    accion           VARCHAR(60)     NOT NULL,
    entidad_afectada VARCHAR(40)     NOT NULL,
    detalle          TEXT            NULL,
    fecha            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_evento        CHAR(36)        NULL,
    PRIMARY KEY (id_auditoria),
    CONSTRAINT uq_auditoria_evento UNIQUE (id_evento),
    INDEX ix_auditoria_fecha (fecha),
    INDEX ix_auditoria_accion_fecha (accion, fecha),
    INDEX ix_auditoria_usuario_fecha (id_usuario, fecha)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
