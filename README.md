# Orvex (beta)

Arma tu PC y comprueba que funcione **antes de comprar**: compatibilidad entre componentes y consumo de la fuente.

> Estado: beta temprana. Los datos del catálogo son de ejemplo y los precios son ilustrativos.

Proyecto del grupo **Los Llajta Bytes** — Universidad Privada Franz Tamayo, Ingeniería de Sistemas.

## Stack

| Capa | Tecnología |
|---|---|
| Frontend | Next.js 14 (React) + TypeScript, sistema de diseño propio (sin Bootstrap) |
| Backend | 9 microservicios Spring Boot 3.3 (Java 21) + API Gateway (Spring Cloud Gateway) |
| Base de datos | MySQL 8 con Flyway: un esquema y un usuario por servicio |
| Mensajería y resiliencia | RabbitMQ (eventos), Resilience4j (Circuit Breaker), Mailpit (correo de prueba) |
| Observabilidad | Prometheus y Grafana (perfil `monitoreo`) |
| Infraestructura | Docker Compose, GitHub Actions y Jenkins |
| Investigación operativa | Cadenas de Markov, política (s, S) y EOQ/ROP en Java puro ([docs/investigacion-operativa.md](docs/investigacion-operativa.md)) |

## Cómo levantarlo

Requisitos: Docker y Docker Compose, con unos **3 GB de RAM libres** para Docker (son 12 contenedores).

```bash
cp .env.example .env      # cambia contraseñas o puertos si los tuyos están ocupados
docker compose up --build
```

La primera vez tarda unos minutos (compila el backend dentro de Docker).

- Web: http://localhost:3000
- API (siempre a través del gateway): http://localhost:8080/api/productos
- Correos de prueba (Mailpit): http://localhost:8025
- Cuentas de demostración, una por rol: `administrador@orvex.demo`, `vendedor@orvex.demo`,
  `almacenero@orvex.demo` y `cliente@orvex.demo`, con contraseña `Orvex2026!` (ver [docs/seguridad.md](docs/seguridad.md))

Los microservicios no se publican en tu máquina: solo se llega a ellos por el gateway.

> Si ya tenías una versión anterior levantada, el volumen de datos cambió de `orvex_db` a `orvex_mysql`
> (el esquema por servicio necesita una base nueva). El volumen viejo no se borra solo:
> cuando ya no lo necesites, elimínalo con `docker volume rm orvex-main_orvex_db`.

### Compartir un link público de prueba (túnel)

La web reenvía `/api` al gateway, así que basta con publicar el puerto de la web. Con
[cloudflared](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/) instalado:

```bash
cloudflared tunnel --url http://localhost:3000
```

Copia la dirección `https://….trycloudflare.com` que muestra, ponla en `.env` como `WEB_URL_PUBLICA=` (para que el
gateway la acepte y los correos usen ese enlace) y aplica el cambio con `docker compose up -d gateway usuarios`.
El link funciona mientras el equipo esté encendido con Docker y cloudflared corriendo, y cambia cada vez que se
abre un túnel nuevo. Las cuentas de demostración quedan accesibles para cualquiera que tenga el link.

### Sin Docker (desarrollo)

```bash
# 1) MySQL 8: ejecuta infra/mysql/init/01-esquemas.sh (crea los esquemas orvex_* y sus usuarios)
# 2) Backend (Java 21 + Maven): compila y prueba todo
cd backend && mvn -B verify
#    Levanta cada servicio que necesites, en su propia terminal:
mvn -pl servicios/catalogo spring-boot:run
mvn -pl servicios/compatibilidad spring-boot:run
mvn -pl gateway spring-boot:run
# 3) Frontend (Node 20)
cd frontend && npm ci && npm run dev   # reenvía /api a http://localhost:8080 (API_INTERNA_URL)
```

## Estructura

```
backend/
  gateway/         API Gateway: único punto de entrada, CORS, timeouts y /api/estado
  common/          Base compartida de los servicios (errores, timeouts HTTP)
  compat-engine/   Motor de compatibilidad: Java puro, sin Spring ni BD, con tests
  servicios/       Un módulo por microservicio: usuarios, catalogo, compatibilidad, optimizacion,
                   inventario, pedidos-pagos, notificaciones, auditoria, reportes
frontend/          Next.js: tienda, armador y carrito
infra/mysql/       Script que crea un esquema y un usuario por servicio
docs/              Plan de implementación y arquitectura (propiedad de las 24 tablas)
docker-compose.yml MySQL + gateway + 9 servicios + web
.github/workflows/ CI: prueba el backend, compila el frontend y construye las imágenes
```

Detalle de servicios, puertos, rutas y tablas: [docs/arquitectura.md](docs/arquitectura.md).
Plan por etapas: [docs/plan-implementacion.md](docs/plan-implementacion.md).
Sistema de diseño: [docs/sistema-diseno.md](docs/sistema-diseno.md) y la guía viva en http://localhost:3000/diseno.

## Cómo funciona el motor de compatibilidad

Cada regla compara una especificación de un componente con la de otro y devuelve
**Compatible**, **Advertencia** o **Incompatible**. Si falta un dato, advierte en lugar de dar la regla por buena.

Reglas iniciales: socket CPU–placa, tipo y frecuencia de RAM, formato placa–gabinete, largo de GPU, altura y socket del
cooler, interfaz M.2/SATA del disco, conectores PCIe de la fuente y consumo vs. fuente (TDP de CPU + GPU + 100 W base, con
30 % de margen; son supuestos a validar).

Las reglas viven en la tabla `regla_compatibilidad` como texto (`a.socket = b.socket`) y el administrador las edita desde
`/admin/reglas` sin tocar el código. Gramática, reglas y API en [docs/compatibilidad.md](docs/compatibilidad.md).

## API

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/productos/buscar?q=&categoria=&marca=&socket=…` | Búsqueda con filtros y paginación (HU-04, RF-04). Detalle en [docs/catalogo.md](docs/catalogo.md) |
| GET | `/api/productos?categoria=PROCESADOR` | Lista productos publicados (HU-04) |
| GET | `/api/productos?ids=1,4,7` | Productos publicados por ID (lo usa el servicio de compatibilidad) |
| GET | `/api/estado` | Salud de cada microservicio (HU-25) |
| POST | `/api/armador/verificar` | Cuerpo: `{"productoIds":[1,4,7]}`. Devuelve el reporte de compatibilidad y el total (HU-06, HU-07) |
| GET/POST/PUT/DELETE | `/api/configuraciones[/{id}]` | Configuraciones guardadas del usuario (HU-08) |
| GET/POST/PUT/DELETE | `/api/reglas[/{id}]` | Reglas de compatibilidad (solo administrador, RNF-06) |
| GET/POST/PATCH/DELETE | `/api/carrito[/items/{id}]` | Carrito de la cuenta (HU-10) |
| POST/GET | `/api/pedidos[/{id}]` | Comprar y seguir pedidos; pago QR y recibo. Detalle en [docs/pedidos.md](docs/pedidos.md) |
| POST/GET | `/api/pos/ventas[/{id}]` | Caja de la tienda: efectivo con cambio o QR, cierre del día. Detalle en [docs/pedidos.md](docs/pedidos.md) |
| GET | `/api/reportes/tablero`, `/api/reportes/exportar/{reporte}.csv` | Tablero e informes del administrador ([docs/reportes.md](docs/reportes.md)) |
| GET/POST | `/api/reportes/markov/...` | Cadenas de Markov: política de reposición, ciclo del pedido y calculadora |
| GET | `/api/auditoria`, `/api/notificaciones` | Auditoría (administrador) y avisos del usuario ([docs/eventos-y-resiliencia.md](docs/eventos-y-resiliencia.md)) |
| POST | `/api/optimizacion/analizar` | Puntaje, recomendaciones y alternativas del armado (HU-09, HU-10). Detalle en [docs/optimizacion.md](docs/optimizacion.md) |

## Estado del proyecto

| Etapa | Contenido | Estado |
|---|---|---|
| 1 · M01 Base técnica | Gateway, 9 servicios en Docker, esquema por servicio, CI | **Hecha** |
| 2 | Sistema de diseño e interfaz: tokens, 24 componentes, inicio, catálogo, configurador y carrito | **Hecha** |
| 3 · M02 | Usuarios: registro, sesión JWT, recuperación por correo, perfil, direcciones, roles y panel de admin | **Hecha** |
| 4 · M03 | Catálogo: modelo del diccionario, búsqueda con filtros, ficha de producto, administración de productos y fotos, stock real | **Hecha** |
| 5 · M03/M07 | Inventario: reservas atómicas con vencimiento, ingresos, ajustes, alertas, proveedores y panel del almacenero | **Hecha** |
| 6 · M04 | Armador: reglas en la base de datos editables por el administrador, M.2/SATA, conectores PCIe, frecuencia de RAM y configuraciones guardadas | **Hecha** |
| 7 · M05 | Optimizador: puntaje 0–100 explicado, balance procesador/gráfica, RAM y doble canal según el uso, y alternativas en stock que vuelven a pasar la compatibilidad | **Hecha** |
| 8 · M05/M06 | Carrito en el servidor, checkout con reserva de stock, pago QR (banco de pruebas) con webhook idempotente, recibo, estados del pedido y preparación | **Hecha** |
| 9 · M06 | Caja (POS) con teclado y pantalla táctil: efectivo con cambio o QR, recibo imprimible y cierre de caja | **Hecha** |
| 10 · M07 | Tablero con indicadores reales y CSV, todas las ventas con anulación, seguimiento del pedido para el cliente | **Hecha** |
| 11 · M07 | RabbitMQ: notificaciones por correo (con reintentos y Circuit Breaker) y auditoría de acciones críticas | **Hecha** |
| 12 · M08 | Circuit Breaker entre servicios, métricas, Grafana, alertas y Jenkinsfile | **Hecha** |
| IO | Investigación operativa: cadenas de Markov aplicadas al inventario y a los pedidos | **Hecha** |
| 13 | QA final: cobertura, OpenAPI, pruebas de punta a punta y en 3 tamaños de pantalla ([docs/pruebas.md](docs/pruebas.md)) | **Hecha** |

Funciona hoy: catálogo con filtros y ficha de producto, armador de PC con verificación de compatibilidad y consumo y configuraciones guardadas, reglas editables por el administrador, análisis de optimización con alternativas en stock, compra con pago QR de prueba y recibo, preparación de pedidos, cuentas con roles, panel de administración de productos y usuarios, e inventario con reservas que impiden vender dos veces la misma unidad. El detalle está en
[docs/plan-implementacion.md](docs/plan-implementacion.md).
