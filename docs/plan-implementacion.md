# ORVEX — Auditoría del repositorio y plan de implementación

Fecha: 2026-11-07 · Fuentes funcionales: especificación completa del proyecto (derivada del informe)
e `Informe_Orvex_APA7.docx` (26 RF, 14 RNF, 27 HU, 8 módulos, 24 tablas).
Fuente visual: "Prompt maestro visual y de implementación".

---

## 1. Estado actual del repositorio

| Parte | Tecnología | Qué hace hoy | Se conserva |
|---|---|---|---|
| `backend/compat-engine` | Java 21 puro | Motor con 7 reglas (socket, RAM, formato, largo GPU, altura cooler, cooler-socket, consumo/fuente). 13 tests. | **Sí**: pasa a ser el núcleo del servicio de Compatibilidad |
| `backend/api` | Spring Boot 3.3 + JPA + Flyway | `GET /api/productos`, `POST /api/armador/verificar`. 10 tests. | **Sí**: se divide en servicios Catálogo y Compatibilidad |
| BD | MySQL 8, 1 tabla `producto` con specs en JSON | 30 productos de ejemplo | Datos de ejemplo sí; el esquema se migra al del informe |
| `frontend` | Next.js 14 + TypeScript + Bootstrap 5 | Inicio, tienda, armador, carrito (localStorage) | **Sí** el framework; la interfaz se rediseña |
| Infra | Docker Compose (db, api, web), GitHub Actions | Levanta todo con un comando | **Sí**, se amplía |

### Cobertura frente al informe

| Módulo | Historias | Estado |
|---|---|---|
| M01 Base técnica y despliegue | HU-22 Gateway, HU-23 Docker, HU-26 CI | Docker y CI parciales; **sin API Gateway** |
| M02 Usuarios y acceso | HU-01, 02, 03, 19 | **No existe** (sin login, JWT, roles) |
| M03 Catálogo e inventario | HU-04, 05, 11 | Catálogo de solo lectura; **sin stock real, admin ni imágenes** |
| M04 Armador y compatibilidad | HU-06, 07 | **Funciona** (7 de 9 reglas; faltan M.2/SATA y conectores de la fuente) |
| M05 Optimización y carrito | HU-08, 09, 10 | Carrito solo en el navegador; **sin optimizador ni alternativas** |
| M06 Ventas, pagos y pedidos | HU-12, 15, 16, 17 | **No existe** (sin pedidos, QR, POS, reservas, recibos) |
| M07 Inventario, notificaciones y reportes | HU-13, 14, 18, 20, 21 | **No existe** |
| M08 Resiliencia, monitoreo y CD | HU-24, 25, 27 | **No existe** (sin métricas, Circuit Breaker ni Jenkins) |

En resumen: hay una beta sólida del armador (~15 % del alcance). Falta casi todo lo transaccional.

---

## 2. Contradicciones y decisiones propuestas

| # | Discrepancia | Decisión propuesta |
|---|---|---|
| D1 | El documento de contexto y el informe coinciden; el contexto pide columnas exactas del diccionario | Usar el diccionario del documento de contexto como esquema oficial |
| D2 | El prompt pide **9 servicios** (incluye Notificaciones, Auditoría y Reportes); el informe nombra Usuarios, Catálogo, Compatibilidad, Optimización, Inventario y Pedidos/Pagos ("Ventas"), con notificaciones vía cola | Implementar los 9: son compatibles con el informe y separan mejor las responsabilidades |
| D3 | El informe dice "**un esquema por servicio**", pero su diagrama físico tiene llaves foráneas entre módulos (p. ej. `venta → usuario`) | Un esquema MySQL por servicio, **sin FK entre esquemas**: se guarda el ID y se valida por API o eventos. Documentado en cada contrato |
| D4 | Hoy las specs están en una columna JSON; el informe usa la tabla `especificacion_tecnica` (filas) y `regla_compatibilidad` | Migrar al modelo del informe: las reglas se cargan desde la BD (RNF-06: cambiar reglas sin desplegar) |
| D5 | El prompt visual tiene 4 objetivos (gaming, edición, productividad, **uso general**); informe y contexto tienen 3 (juegos, edición, oficina) | Implementar los 3 oficiales. "Uso general" queda como extensión opcional, desactivada, hasta que el equipo lo apruebe |
| D6 | El prompt pide "**Ofertas**" y "más vendidos"; el informe no tiene descuentos | "Más vendidos" se calcula de ventas reales (se oculta si no hay). "Ofertas" requiere agregar `precio_oferta` y vigencia a `producto` (**cambio al modelo; requiere tu aprobación**) o se omite |
| D7 | El prompt pide **valoraciones** y popularidad | No hay datos en el informe: **no se implementan** |
| D8 | Umbrales sin definir: margen de la fuente, puntaje del optimizador, minutos de la reserva, stock mínimo | Parámetros configurables por variable de entorno o tabla. Valores iniciales: margen 30 %, reserva 15 min, stock mínimo por producto. Documentados como propuestos, no medidos |
| D9 | QR bancario sin proveedor definido | Puerto `ProveedorQr` + adaptador **Sandbox** claramente marcado "MODO PRUEBA". El pago solo se confirma por webhook firmado (HMAC) e idempotente por `referencia_qr`; nunca con un botón del cliente |
| D10 | Imágenes de producto: no hay fotos con licencia | Gestión de fotos en el panel admin (subida real). Las semillas usan ilustraciones neutras por categoría, marcadas como ejemplo |
| D11 | El informe no fija tecnologías concretas | Se mantiene lo existente: Spring Boot 3 / Java 21 y Next.js 14. Se agregan **Spring Cloud Gateway**, **RabbitMQ** (cola), **Mailpit** (correo de desarrollo), **Prometheus + Grafana + Loki** (observabilidad), **Resilience4j** (Circuit Breaker) y **Jenkins** |
| D12 | Bootstrap 5 contra la identidad visual propia que pide el prompt | Reemplazar Bootstrap por un sistema de diseño propio (tokens CSS + componentes React). Razón: Bootstrap impone su estética y obliga a pelear con sus estilos |
| D13 | El carrito vive en localStorage; RF-11 lo asocia al usuario | Carrito en el servidor para usuarios con sesión; el del visitante se fusiona al iniciar sesión |

---

## 3. Arquitectura objetivo

```
Navegador ──► web (Next.js: tienda, armador, cliente, admin, POS)
                 │  REST/JSON + JWT
                 ▼
            gateway (Spring Cloud Gateway: valida JWT y rol, enruta, rate limit)
   ┌──────┬──────────┬──────────────┬─────────────┬───────────┬──────────────┐
usuarios catalogo compatibilidad optimizacion inventario pedidos-pagos
   │                                                          │
   └──── eventos (RabbitMQ) ──► notificaciones · auditoria · reportes
MySQL 8: un esquema por servicio · Prometheus/Grafana/Loki · GitHub Actions + Jenkins
```

| Servicio | Tablas del informe | Responsabilidad |
|---|---|---|
| usuarios | rol, usuario, direccion | Registro, login (JWT, bcrypt), recuperación, perfil, roles |
| catalogo | categoria, producto, especificacion_tecnica, proveedor | Productos, specs, fotos, búsqueda y filtros |
| compatibilidad | regla_compatibilidad, configuracion_pc, item_configuracion, resultado_compatibilidad | Motor de reglas (compat-engine) y configuraciones guardadas |
| optimizacion | analisis_optimizacion, alternativa_sugerida | Puntaje 0–100, cuellos de botella, alternativas en stock |
| inventario | stock, movimiento_inventario, reserva_stock | Stock con versión optimista, reservas con vencimiento, ingresos y ajustes, alertas |
| pedidos-pagos | carrito, item_carrito, venta, detalle_venta, pago, recibo | Carrito, checkout, POS, QR idempotente, recibos, ciclo del pedido |
| notificaciones | notificacion | Correos por eventos de la cola |
| auditoria | auditoria | Registro de acciones críticas por eventos |
| reportes | (lectura) | Ventas por día, canal y producto; inventario |

Concurrencia (RNF-05): toda reserva y descuento se hace en `inventario` con `UPDATE ... WHERE disponible >= ? AND version = ?` y el CHECK `cantidad_reservada <= cantidad_disponible`. La web y el POS pasan por el mismo endpoint, así que es imposible vender unidades inexistentes. Se cubrirá con una prueba de compras concurrentes.

---

## 4. Plan por etapas (sigue los sprints M01–M08 del informe)

Cada etapa termina con: build verde, tests, prueba manual en el navegador y un commit o PR.

| Etapa | Contenido | Resultado verificable |
|---|---|---|
| **0. Preparación** ✅ | Repo git local enlazado a `vasusjelpro/Orvex`; rama por etapa | Historial limpio por etapa |
| **1. Base (M01)** ✅ | Monorepo de servicios, gateway, esquemas por servicio, compose ampliado, CI por servicio | `docker compose up` levanta gateway + servicios con health checks |
| **2. Sistema de diseño** ✅ | Tokens (paleta del prompt), tipografía, los 24 componentes base, movimiento reducido, layouts claro (tienda) y oscuro (armador, admin, POS) | Página de componentes y el inicio rediseñado |
| **3. Usuarios (M02)** ✅ | Registro, login JWT, recuperación por correo (Mailpit), perfil, direcciones, RBAC en el gateway | Login por rol que redirige a la pantalla correcta |
| **4. Catálogo (M03)** ✅ | Migración de specs a filas, fotos, filtros (marca, socket, tipo de RAM, precio), paginación, detalle de producto, CRUD admin con estados borrador/publicado/inactivo | Catálogo y ficha de producto con datos del backend |
| **5. Inventario (M03/M07)** ✅ | Stock, reservas, ingresos y ajustes, movimientos, alertas de stock bajo | Prueba de concurrencia: dos compras del último ítem, solo una gana |
| **6. Armador y compatibilidad (M04)** ✅ | Configurador por etapas, reglas en BD, las 2 reglas faltantes (M.2/SATA y conectores), configuraciones guardadas | Armador nuevo; las incompatibilidades bloquean la compra |
| **7. Optimizador (M05)** ✅ | Puntaje, balance CPU/GPU, RAM y doble canal, alternativas en stock, con explicación de cada recomendación | Pantalla de optimización con reglas documentadas |
| **8. Carrito, checkout y QR (M05/M06)** ✅ | Carrito en servidor, checkout, reserva, QR sandbox, webhook idempotente, recibo, estados del pedido | Pago de prueba completo; el webhook repetido no duplica |
| **9. POS (M06)** ✅ | Caja rápida (teclado y táctil), efectivo con vuelto o QR, recibo imprimible | Venta física que descuenta del mismo stock |
| **10. Panel cliente y admin (M07)** ✅ | Mis pedidos, pagos y configuraciones; dashboard con KPIs reales, pedidos, usuarios y roles, reglas, reportes, auditoría | Indicadores leídos de la BD |
| **11. Notificaciones, auditoría y reportes** ✅ | Consumidores de RabbitMQ, correos, registro de auditoría | Correos visibles en Mailpit; auditoría de cambios de precio y stock |
| **12. Resiliencia y observabilidad (M08)** ✅ | Resilience4j, métricas, Grafana y alertas, panel "Estado de servicios", Jenkinsfile | Al apagar `notificaciones`, la compra sigue funcionando |
| **IO. Investigación operativa** ✅ | Cadenas de Markov: política de reposición (s, S) por producto, ciclo del pedido como cadena absorbente y calculadora ([investigacion-operativa.md](investigacion-operativa.md)) | Resultados comparados con ejemplos de libro |
| **13. QA final** ✅ | Cobertura ≥ 70 % (RNF-12), OpenAPI por servicio, pruebas en 3 tamaños de pantalla, documentación | Informe de pruebas con evidencia |

### Esfuerzo y riesgos

- El informe estima **465 horas**. No cabe en una sola sesión: son muchas sesiones de trabajo, etapa por etapa.
- Ejecutar 9 servicios Java, gateway, MySQL, RabbitMQ y monitoreo necesita unos **6–8 GB de RAM** para Docker. Se limitará el heap de cada JVM; si no alcanza, se pueden levantar perfiles parciales.
- El repositorio de GitHub aún no tiene permiso de escritura para esta cuenta: el trabajo se guarda en git local y se sube cuando haya acceso.
