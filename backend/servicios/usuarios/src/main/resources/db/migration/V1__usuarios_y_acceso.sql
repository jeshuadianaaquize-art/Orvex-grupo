-- Módulo de datos "Usuarios y acceso" (diccionario de datos, tablas 1 a 3). MySQL 8, InnoDB, utf8mb4.
-- Las tablas notificacion y auditoria pertenecen a sus propios servicios (docs/arquitectura.md).

CREATE TABLE rol (
    id_rol      TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(20)  NOT NULL,
    descripcion VARCHAR(100) NULL,
    PRIMARY KEY (id_rol),
    UNIQUE KEY uk_rol_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE usuario (
    id_usuario      INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    id_rol          TINYINT UNSIGNED NOT NULL,
    nombre          VARCHAR(80)      NOT NULL,
    correo          VARCHAR(100)     NOT NULL,
    contrasena_hash VARCHAR(255)     NOT NULL,
    telefono        VARCHAR(20)      NULL,
    estado          ENUM('activo','bloqueado') NOT NULL DEFAULT 'activo',
    fecha_registro  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uk_usuario_correo (correo),
    KEY idx_usuario_rol (id_rol),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE direccion (
    id_direccion INT UNSIGNED NOT NULL AUTO_INCREMENT,
    id_usuario   INT UNSIGNED NOT NULL,
    calle        VARCHAR(120) NOT NULL,
    ciudad       VARCHAR(50)  NOT NULL,
    referencia   VARCHAR(150) NULL,
    PRIMARY KEY (id_direccion),
    KEY idx_direccion_usuario (id_usuario),
    CONSTRAINT fk_direccion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Los cuatro roles del informe (RF-21). Son datos del sistema, no de demostración.
INSERT INTO rol (nombre, descripcion) VALUES
('cliente',       'Compra en línea, arma su PC y consulta sus pedidos'),
('vendedor',      'Registra ventas en la tienda física (punto de venta)'),
('almacenero',    'Gestiona inventario, ingresos, ajustes y prepara pedidos'),
('administrador', 'Gestiona productos, usuarios, roles, reportes y el sistema');
