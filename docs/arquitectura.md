# Arquitectura de Orvex

Implementa la arquitectura del informe (punto 5.3.1.12): microservicios por capas, API Gateway como único
punto de entrada, un contenedor Docker y un esquema de base de datos por servicio.

```
                Navegador
                    │
          web (Next.js, puerto 3000)
                    │  REST/JSON
                    ▼
     gateway (Spring Cloud Gateway, puerto 8080 → API_PORT)
     CORS · timeouts · /api/estado · valida JWT y rol
                    │  red interna de Docker
 ┌─────────┬────────┼─────────────┬─────────────┬──────────┐
usuarios catalogo compatibilidad optimizacion inventario pedidos-pagos
 :8101    :8102  ◄─── :8103         :8104       :8105      :8106
          (REST)                                            │
                    notificaciones :8107 · auditoria :8108 · reportes :8109
                    [M07: consumen eventos de la cola de mensajes]
                    │
           MySQL 8 · 9 esquemas · 9 usuarios (cada uno solo ve su esquema)
```

## Servicios

| Servicio | Módulo Maven | Puerto | Rutas en el gateway | Estado |
|---|---|---|---|---|
| gateway | `backend/gateway` | 8080 | — · `GET /api/estado` | Enrutamiento, CORS, timeouts, salud de servicios, **validación de JWT y roles** |
| usuarios | `servicios/usuarios` | 8101 | `/api/auth/**`, `/api/usuarios/**`, `/api/roles/**` | **Funciona**: registro, sesión JWT, recuperación, perfil, direcciones, usuarios y roles ([seguridad.md](seguridad.md)) |
| catalogo | `servicios/catalogo` | 8102 | `/api/productos/**`, `/api/categorias/**`, `/api/proveedores/**`, `/api/catalogo/**` | **Funciona**: búsqueda con filtros, ficha, administración de productos, especificaciones y fotos ([catalogo.md](catalogo.md)) |
| compatibilidad | `servicios/compatibilidad` | 8103 | `/api/armador/**`, `/api/configuraciones/**`, `/api/reglas/**` | **Funciona**: verificación (llama al catálogo por REST), reglas en la tabla `regla_compatibilidad` y configuraciones guardadas ([compatibilidad.md](compatibilidad.md)) |
| optimizacion | `servicios/optimizacion` | 8104 | `/api/optimizacion/**` | **Funciona**: puntaje, recomendaciones y alternativas; llama al catálogo y a la API interna de compatibilidad ([optimizacion.md](optimizacion.md)) |
| inventario | `servicios/inventario` | 8105 | `/api/inventario/**` (reservas: solo API interna) | **Funciona**: existencias, reservas atómicas con vencimiento, ingresos, devoluciones, ajustes y alertas ([inventario.md](inventario.md)) |
| pedidos-pagos | `servicios/pedidos-pagos` | 8106 | `/api/carrito/**`, `/api/pedidos/**`, `/api/pagos/**`, `/api/pos/**`, `/api/recibos/**` | **Funciona**: carrito, checkout con reserva, pago QR con webhook idempotente, recibo, preparación, **caja (POS)** y administración de ventas ([pedidos.md](pedidos.md)) |
| notificaciones | `servicios/notificaciones` | 8107 | `/api/notificaciones/**` | **Funciona**: avisos por correo desde la cola, con reintentos y Circuit Breaker ([eventos-y-resiliencia.md](eventos-y-resiliencia.md)) |
| auditoria | `servicios/auditoria` | 8108 | `/api/auditoria/**` | **Funciona**: registro de acciones críticas desde la cola, solo lectura |
| reportes | `servicios/reportes` | 8109 | `/api/reportes/**` | **Funciona**: tablero, exportación CSV e **investigación operativa** con cadenas de Markov ([investigacion-operativa.md](investigacion-operativa.md)) |

Infraestructura: MySQL 8, RabbitMQ 3.13 (cola de eventos), Mailpit (correo de prueba) y, con el perfil `monitoreo`,
Prometheus y Grafana.

Módulos compartidos:
- `compat-engine`: motor de reglas en Java puro (lo usan compatibilidad y optimización, con las mismas reglas de la base de datos).
- `common`: dependencias base, formato de errores `{"error": "...", "campos": {...}}` y un `RestClient` con timeouts.
- `seguridad-jwt`: emisión y validación de JWT (Java puro), compartido por usuarios y gateway.
- `eventos`: publicación de eventos de negocio en RabbitMQ después de confirmar la transacción, sin bloquear.
- `investigacion-operativa`: cadenas de Markov, política de inventario (s, S) y EOQ/ROP en Java puro.

## Propiedad de las 24 tablas del diccionario

El informe pide "un esquema por servicio", pero su diagrama físico tiene llaves foráneas entre módulos.
**Decisión (D3):** cada tabla vive en el esquema de un solo servicio. Entre esquemas no hay FK: se guarda el ID
y se valida por API o por eventos. Las FK *dentro* de un mismo esquema se mantienen como en el diccionario.

| Esquema | Servicio | Tablas | Referencias a otros servicios (sin FK) |
|---|---|---|---|
| `orvex_usuarios` | usuarios | rol, usuario, direccion | — |
| `orvex_catalogo` | catalogo | categoria, producto, especificacion_tecnica, proveedor | — |
| `orvex_inventario` | inventario | stock, movimiento_inventario, reserva_stock | id_producto (catálogo), id_proveedor (catálogo), id_usuario (usuarios) |
| `orvex_compatibilidad` | compatibilidad | regla_compatibilidad, configuracion_pc, item_configuracion, resultado_compatibilidad | id_usuario, id_producto |
| `orvex_optimizacion` | optimizacion | analisis_optimizacion, alternativa_sugerida | id_configuracion (compatibilidad), id_producto |
| `orvex_pedidos` | pedidos-pagos | carrito, item_carrito, venta, detalle_venta, pago, recibo | id_cliente, id_vendedor (usuarios), id_producto, id_configuracion; la dirección se copia como texto (D39) |
| `orvex_notificaciones` | notificaciones | notificacion | id_usuario |
| `orvex_auditoria` | auditoria | auditoria | id_usuario |
| `orvex_reportes` | reportes | — (D48: sin copia de datos) | Consulta en el momento las APIs internas de pedidos e inventario |

Total: 3 + 4 + 3 + 4 + 2 + 6 + 1 + 1 = **24 tablas**, igual que el diccionario.

Estado actual: las 24 tablas del diccionario están implementadas. Extensiones documentadas: `venta.direccion_entrega`
(D39), `seguimiento_venta` (D46), `pago.monto_recibido` (D47) e `id_evento` en auditoría y notificaciones (D50).

## Comunicación entre servicios

| Origen → destino | Tipo | Para qué | Si el destino falla |
|---|---|---|---|
| navegador → web → gateway | REST: el navegador llama a `/api` del mismo dominio y Next.js lo reenvía al gateway (`API_INTERNA_URL`) | Todas las peticiones | La web muestra el mensaje de error |
| gateway → servicio | REST (proxy) | Enrutamiento | 5xx en esa ruta; el resto sigue funcionando (timeout de conexión 2 s) |
| compatibilidad → catálogo | REST `GET /api/productos?ids=` | Especificaciones y precio de los productos del armado | 503 "El servicio de catálogo no está disponible…" (timeouts 2 s / 5 s) |
| optimización → catálogo | REST `GET /api/productos` | Especificaciones, precio y stock para el análisis y las alternativas | 503 "El servicio de catálogo no está disponible…" |
| optimización → compatibilidad | REST `GET /internal/reglas` (caché 30 s), `GET /internal/configuraciones/{id}?usuario=` | Mismas reglas que el armador; configuraciones guardadas del usuario | 503; una configuración ajena da 404 |
| pedidos → inventario | REST `/internal/reservas` (crear, confirmar, liberar) | Reserva todo o nada, descuento al pagar, liberación al vencer o cancelar | 503 al confirmar la compra; el webhook responde 503 y el banco reintenta |
| pedidos → catálogo, compatibilidad, usuarios | REST (`/api/productos?ids=`, `/internal/configuraciones/{id}`, `/internal/usuarios/{id}/…`) | Precios, PC completas verificadas, dirección y nombre para el recibo | 503 al comprar; los nombres de un pedido viejo se muestran como "Producto #id" |
| banco QR → pedidos | Webhook `POST /api/pagos/webhook` firmado (HMAC-SHA256) | Confirmación asíncrona del pago | Reintentos del banco; idempotente |
| inventario → catálogo | REST `GET /internal/productos`, `/internal/proveedores/{id}` | Nombres de producto y validación de proveedor | 503 en el panel de inventario |
| catálogo → inventario | REST `GET /internal/stock?ids=` | Unidades disponibles de cada producto | Disponibilidad "por confirmar" (`stock: null`) |
| gateway → usuarios | REST `GET /internal/usuarios/{id}/acceso` (caché 30 s) | Estado y rol actuales de quien llama | Usa el rol del token (modo degradado) |
| usuarios → Mailpit/SMTP | SMTP en segundo plano | Bienvenida y recuperación | El registro no falla; se registra un aviso |

| reportes → pedidos, inventario, catálogo | REST `/internal/reportes/ventas`, `/internal/seguimiento`, `/internal/demanda/{p}`, `/internal/resumen` | Tablero, CSV y modelos de Markov | El tablero se muestra sin la parte caída (aviso); Markov responde 503 |
| notificaciones → usuarios | REST `/internal/usuarios?rol=`, `/internal/usuarios/{id}/contacto` | Destinatarios de los avisos | Los avisos esperan en la tabla |
| servicios → RabbitMQ → notificaciones, auditoría | Eventos (exchange topic `orvex.eventos`) | Avisos y auditoría | Ver [eventos-y-resiliencia.md](eventos-y-resiliencia.md): nada bloquea la compra |

Todas las llamadas REST entre servicios llevan timeouts y un **Circuit Breaker por servicio destino** (Resilience4j,
en el módulo `common`).

## Variables de entorno

| Variable | Dónde | Por defecto | Uso |
|---|---|---|---|
| `DB_PASSWORD` | db, servicios | `orvex_dev` | Contraseña de los 9 usuarios de BD |
| `DB_ROOT_PASSWORD` | db | `root_dev` | Contraseña de root de MySQL |
| `DB_PORT`, `API_PORT`, `WEB_PORT` | compose | 3306, 8080, 3000 | Puertos publicados en tu máquina |
| `DB_HOST`, `DB_PUERTO` | servicios | `localhost`, 3306 | Conexión a MySQL |
| `SERVER_PORT` | servicios | ver tabla de servicios | Puerto de cada servicio |
| `*_URL` (p. ej. `CATALOGO_URL`) | gateway, compatibilidad | `http://localhost:81xx` | Dirección interna de cada servicio |
| `CORS_ORIGINS` | gateway | `http://localhost:3000` (+ `WEB_URL_PUBLICA`) | Orígenes web permitidos (separados por coma) |
| `API_INTERNA_URL` | web (al compilar) | `http://gateway:8080` | Gateway al que la web reenvía `/api` |
| `WEB_URL_PUBLICA` | compose | — | Dirección pública (túnel o dominio): CORS y enlaces de los correos |
| `INVENTARIO_URL`, `FOTOS_DIR` | catálogo | `http://localhost:8105`, `./datos/fotos` | Inventario y carpeta de fotos (volumen `orvex_fotos` en Docker) |
| `JWT_SECRET` | gateway, usuarios | (obligatoria en Compose) | Secreto de firma de las sesiones, 32+ caracteres |
| `SMTP_HOST`, `SMTP_PORT` | usuarios | `mailpit`, 1025 | Servidor de correo |
| `WEB_URL` | usuarios | `http://localhost:3000` | Base de los enlaces de los correos |
| `ORVEX_DEMO`, `ORVEX_DEMO_CONTRASENA` | usuarios, web | `true`, `Orvex2026!` | Cuentas de demostración (solo desarrollo) |
| `RABBIT_HOST`, `RABBIT_USUARIO`, `RABBIT_CLAVE`, `EVENTOS_HABILITADOS` | servicios | `rabbitmq`, `orvex`, `orvex_dev`, `true` en Compose | Cola de eventos (en pruebas, apagada: solo log) |
| `ZONA_HORARIA` | pedidos, reportes, auditoría | `America/La_Paz` | Días del cierre de caja y de los reportes (las fechas se guardan en UTC) |
| `GRAFANA_CLAVE`, `PROMETHEUS_PORT`, `GRAFANA_PORT` | monitoreo | `orvex_dev`, 9090, 3001 | Perfil `monitoreo` |

## Verificación de la etapa 1 (M01)

Comprobado con `docker compose up --build` el 2026-11-07:
- 12 contenedores en marcha y 11 health checks en verde; Orvex usa ~2,3 GB de RAM.
- 9 esquemas y 9 usuarios; `orvex_compatibilidad` recibe `SELECT command denied` al leer `orvex_catalogo`.
- El armador verifica a través de gateway → compatibilidad → catálogo, desde la web y desde curl.
- Catálogo detenido a propósito: el armador responde 503 con mensaje claro, `/api/estado` marca `DEGRADADO`
  y el resto de servicios sigue respondiendo. Al reiniciarlo vuelve a `UP` sin intervención.
- Pruebas automáticas: 29 (motor 11, gateway 5, catálogo 5, compatibilidad 8).
