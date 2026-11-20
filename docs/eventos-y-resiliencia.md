# Eventos, notificaciones, auditoría, resiliencia y observabilidad (M07 / M08)

Etapas 11 y 12 del plan. Implementa RF-20 (notificaciones), RF-26 (auditoría), RF-23 (panel de estado y métricas),
RF-25 (despliegue), HU-14, HU-18, HU-21, HU-24, HU-25 y HU-27.

## 1. Eventos de negocio por RabbitMQ

```
usuarios · catálogo · compatibilidad · inventario · pedidos ──► exchange "orvex.eventos" (topic)
                                                                    ├─ "#" ───────────────► cola orvex.auditoria ──► auditoría
                                                                    └─ pago.confirmado ─┐
                                                                       pedido.*        ├─► cola orvex.notificaciones ──► correos
                                                                       stock.bajo     ─┘
```

Módulo `backend/eventos` (lo usan los servicios que publican y los que consumen):

- `PublicadorEventos.publicar(tipo, usuario, entidad, id, datos)`: el evento sale **después de confirmar la
  transacción** (un pago que se revierte no avisa nada) y en **otro hilo** (la compra no espera a RabbitMQ).
- Si RabbitMQ no responde, el evento queda en memoria y se reintenta cada 10 s, en orden (hasta 2000).
  Limitación documentada: si además el servicio se reinicia, esos eventos se pierden (el log conserva la marca `EVENTO`).
  La alternativa robusta es una tabla *outbox* por servicio; no se agregó para no sumar tablas fuera del diccionario.
- RabbitMQ caído **no marca al servicio como caído** (`management.health.rabbit.enabled=false`): si el health check
  fallara, Docker reiniciaría el contenedor y la compra dejaría de funcionar.
- Las colas son durables. Un mensaje que falla 3 veces va a `<cola>.fallidos` (no se pierde ni bloquea la cola).

| Evento | Publica | Datos |
|---|---|---|
| `producto.creado`, `producto.precio_cambiado`, `producto.estado_cambiado` | catálogo | anterior / nuevo |
| `usuario.rol_cambiado`, `usuario.estado_cambiado` | usuarios | correo, anterior / nuevo |
| `regla.creada`, `regla.modificada`, `regla.eliminada` | compatibilidad | nombre, condición |
| `inventario.ingreso`, `inventario.ajuste`, `inventario.devolucion`, `inventario.minimo_cambiado` | inventario | cantidad, motivo, disponible |
| `stock.bajo` | inventario | nombre, disponible, mínimo (como máximo uno cada 12 h por producto, salvo que llegue a 0) |
| `pedido.creado`, `pago.confirmado`, `pedido.estado_cambiado`, `pedido.cancelado` | pedidos | canal, cliente, vendedor, total, estado, recibo |
| `venta.anulada`, `pago.conciliacion` | pedidos | motivo, monto, "reembolsar" |

## 2. Auditoría (`auditoria`, tabla 5 del diccionario)

Guarda **todos** los eventos: acción, entidad, usuario, fecha y el detalle en JSON. Solo agrega filas: no hay API
para modificar ni borrar. *Admin → Auditoría* filtra por acción y fechas.

**D50.** Se agrega `auditoria.id_evento` (UNIQUE) para que una entrega repetida de la cola no duplique el registro, e
`id_usuario` admite NULL para las acciones del sistema (webhook del banco, vencimientos). Lo mismo en `notificacion`
con UNIQUE (`id_evento`, `id_usuario`).

## 3. Notificaciones (`notificacion`, tabla 4 del diccionario)

| Evento | Aviso | Para |
|---|---|---|
| `pago.confirmado` (en línea) | tipo `pago`: monto y número de recibo | El cliente |
| `pedido.estado_cambiado` (preparado, entregado) | tipo `estado_pedido` | El cliente |
| `pedido.cancelado` | tipo `estado_pedido` con el motivo | El cliente |
| `stock.bajo` | tipo `stock_bajo` | Cada almacenero y administrador activo |

Las ventas en tienda no avisan (el cliente está presente). Bienvenida y recuperación las sigue enviando el servicio
de usuarios en el acto: el enlace secreto de recuperación no debe pasar por la cola ni guardarse en una tabla.
El cliente ve sus avisos en *Mi cuenta → Avisos*.

**Flujo de falla del informe** — "hasta 3 reintentos con timeout; abrir Circuit Breaker, guardar correo en cola y
alertar monitoreo; al recuperarse, cerrar el circuito y procesar pendientes; la compra no se bloquea":

1. Cada aviso se guarda primero `pendiente`. Una tarea lo envía cada 5 s con **3 intentos** (timeouts SMTP 3–5 s).
2. Con 5 fallas en las últimas 10 llamadas, el **circuito se abre** 30 s: no se intenta y los avisos esperan en la
   tabla. El cambio de estado queda en el log como `ALERTA_CORREO` y el panel de estado lo muestra.
3. Pasados 30 s deja pasar 2 envíos de prueba; si salen, el circuito se cierra y se envían **todos los pendientes**.
4. Un usuario sin correo queda `fallido`. Si el servicio de usuarios no responde, el aviso espera.

`NotificacionesMysqlTest` lo comprueba con el servidor de correo simulado caído: 15 intentos (5 × 3), circuito
abierto, 6 avisos en espera, y al volver se envían los 6 y el circuito se cierra.

## 4. Resiliencia entre servicios (Circuit Breaker, HU-24)

Todas las llamadas REST entre microservicios pasan por el `RestClient` del módulo `common`, que ahora lleva un
**Circuit Breaker por servicio destino** (`CircuitoPorServicio`, Resilience4j), además de los timeouts (2 s de
conexión, 5 s de respuesta). Cuenta como falla un error de red o un 5xx (un 4xx, como "no hay stock", es una
respuesta válida). Con el circuito abierto la llamada falla en el acto y el cliente responde 503 "el servicio X no
está disponible", sin esperar el timeout en cada pedido.

**D51 — configuración propuesta, no medida:** ventana de 20 llamadas, mínimo 10, 50 % de fallas (u 80 % de llamadas
de más de 4 s) abre el circuito 15 s; luego 3 llamadas de prueba.

Qué pasa si cae cada pieza:

| Cae | Efecto |
|---|---|
| notificaciones | Las compras siguen. Los eventos esperan en la cola durable de RabbitMQ y se procesan al volver |
| RabbitMQ | Las compras siguen. Los eventos esperan en memoria del servicio que los publica y salen al volver |
| servidor de correo | Los avisos esperan en la tabla (circuito abierto) y salen al volver |
| auditoría o reportes | Nada más se afecta; su pantalla muestra el error |
| catálogo | Tienda y armador muestran "no disponible"; inventario y pedidos responden 503 en lo que necesita el catálogo |
| inventario | El catálogo muestra la disponibilidad "por confirmar"; no se puede comprar (no se vende sin reservar) |

## 5. Observabilidad (HU-25, RF-23)

- Cada servicio expone `/actuator/prometheus` en la red interna (el gateway en el puerto de gestión 8090, que no se
  publica). Las métricas llevan la etiqueta `servicio`.
- Perfil de Compose `monitoreo` (para no consumir memoria cuando no se usa):
  `docker compose --profile monitoreo up -d` → Prometheus en `:9090`, Grafana en `:3001` (admin / `GRAFANA_CLAVE`).
- Tablero de Grafana "Orvex · Estado de los microservicios": servicios en línea, circuitos abiertos y su detalle,
  peticiones por segundo, % de errores 5xx, latencia p95 y memoria, por servicio.
- Alertas de Prometheus (`infra/prometheus/alertas.yml`, umbrales propuestos): servicio caído 1 min, circuito abierto,
  más de 5 % de 5xx, p95 mayor a 2 s, memoria mayor a 90 %.
- *Admin → Estado de servicios* muestra la salud de cada servicio, la latencia y la cola de correos con el estado
  de su circuito.

## 6. Despliegue con Jenkins (HU-27)

`Jenkinsfile`: pruebas y cobertura del backend → compilación del frontend → imágenes Docker → **aprobación manual**
→ `docker compose up -d` → prueba de humo. El `.env` del entorno llega como credencial de Jenkins ("orvex-env").
Comparación: GitHub Actions valida cada push y PR en la nube sin desplegar; Jenkins corre en el servidor propio,
despliega y deja una aprobación humana antes de tocar el entorno.
