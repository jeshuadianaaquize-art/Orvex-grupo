-- Tabla 4 del diccionario (esquema orvex_notificaciones). id_usuario vive en orvex_usuarios: sin FK (D3).
-- Extensión D50: id_evento identifica el evento de la cola que la originó; UNIQUE (id_evento, id_usuario) evita
-- avisar dos veces si RabbitMQ entrega dos veces el mismo mensaje (un evento puede avisar a varios usuarios).
CREATE TABLE notificacion (
    id_notificacion INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario      INT UNSIGNED NOT NULL,
    tipo            ENUM('bienvenida','recuperacion','pago','estado_pedido','stock_bajo') NOT NULL,
    mensaje         VARCHAR(255) NOT NULL,
    fecha_envio     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_envio    ENUM('pendiente','enviado','fallido') NOT NULL DEFAULT 'pendiente',
    id_evento       CHAR(36)     NULL,
    PRIMARY KEY (id_notificacion),
    CONSTRAINT uq_notificacion_evento UNIQUE (id_evento, id_usuario),
    INDEX ix_notificacion_usuario_estado (id_usuario, estado_envio),
    INDEX ix_notificacion_estado (estado_envio, id_notificacion)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
