# Catálogo (M03)

Implementa HU-04 y HU-05 (RF-03, RF-04, RF-05; RNF-06) y adelanta la tabla `stock` de HU-11.

## Modelo de datos

La migración `V4__modelo_del_diccionario.sql` llevó el catálogo de la beta al diccionario de datos,
**conservando los id de producto** (los carritos guardados siguen valiendo):

| Tabla (esquema) | Cambio |
|---|---|
| `categoria` (orvex_catalogo) | Nueva, con las 8 categorías del informe |
| `producto` (orvex_catalogo) | `id_categoria` (FK), `foto_url`, `estado` ENUM `'borrador'/'publicado'/'inactivo'`; se quitan `categoria` (texto), `specs` (JSON) y `en_stock` |
| `especificacion_tecnica` (orvex_catalogo) | Nueva: un par nombre/valor por fila, UNIQUE (producto, nombre). Las 69 especificaciones de la beta se migraron desde el JSON |
| `proveedor` (orvex_catalogo) | Nueva y vacía; la usa el ingreso de mercadería (etapa de inventario) |
| `stock` (orvex_inventario) | Nueva en el servicio de inventario, con existencias de **demostración** para los 30 productos |

## API pública (sin sesión)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/productos` | Catálogo publicado completo (`?categoria=`) o por `?ids=` (máx. 50) |
| GET | `/api/productos/buscar` | Filtros `q`, `categoria`, `marca` (varias), `precioMin`, `precioMax`, `socket`, `tipoRam`, `disponible`, `orden` (`PRECIO_ASC`, `PRECIO_DESC`, `NOMBRE`), `pagina`, `tamano` (máx. 48). Devuelve resultados, facetas y si la disponibilidad está confirmada |
| GET | `/api/productos/{id}` | Ficha + 4 relacionados (misma categoría, precio más cercano). 404 si no está publicado |
| GET | `/api/categorias` | Las 8 categorías con cantidad de productos y precio "desde" |
| GET | `/api/catalogo/fotos/{archivo}` | Foto de producto (caché 30 días) |

Cada producto trae `stock` (unidades vendibles = existentes − reservadas) y `enStock`. Ambos son `null` si el
servicio de inventario no respondió: la tienda muestra "Disponibilidad por confirmar" (modo degradado).

## API de administración (rol Administrador)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/catalogo/admin/productos` | Todos los productos, en cualquier estado, con las especificaciones requeridas que faltan |
| GET/POST/PUT | `/api/catalogo/admin/productos[/{id}]` | Crear (nace en borrador) y editar. SKU único (409) |
| PATCH | `/api/catalogo/admin/productos/{id}/estado` | `{estado}`: borrador → publicado → inactivo → publicado. Otras transiciones: 409 |
| POST | `/api/catalogo/admin/productos/{id}/foto` | multipart `archivo`: JPG, PNG o WEBP, máx. 2 MB |
| GET | `/api/catalogo/admin/especificaciones` | Especificaciones conocidas por categoría (tipo, unidad, requerida) |

## Especificaciones por categoría

Definidas en `servicios/catalogo/.../Categoria.java`. Las **requeridas** son las que usa la verificación de
compatibilidad: sin ellas un producto no se puede publicar, ni un producto publicado puede quedar sin ellas.

| Categoría | Requeridas | Opcionales conocidas |
|---|---|---|
| Procesador | socket, tdp_w | nucleos, hilos, frecuencia_ghz |
| Placa madre | socket, tipo_ram, factor_forma | ranuras_ram, ranuras_m2 |
| Memoria RAM | tipo_ram | capacidad_gb, frecuencia_mhz, modulos |
| Tarjeta gráfica | largo_mm, tdp_w | vram_gb |
| Almacenamiento | interfaz | capacidad_gb |
| Fuente de poder | potencia_w | certificacion |
| Gabinete | factores_forma (lista), max_largo_gpu_mm, max_altura_cooler_mm | — |
| Refrigeración | altura_mm, sockets (lista) | — |

Los valores numéricos se validan como números (acepta coma decimal) y las listas se normalizan
(`" ATX , mATX,,ATX "` → `ATX,mATX`). Se pueden agregar especificaciones no listadas (RNF-06).

## Decisiones

| # | Tema | Decisión |
|---|---|---|
| D20 | Descripción y galería | El diccionario no tiene descripción y solo tiene **una** `foto_url`. No se agregaron columnas: la ficha muestra la foto y las especificaciones. Una galería o descripción requieren ampliar el modelo (decisión del equipo) |
| D21 | Búsqueda | Los filtros se aplican en memoria sobre los productos publicados, porque la disponibilidad viene de otro servicio. Sirve para miles de productos; más allá conviene un índice de búsqueda |
| D22 | Especificaciones como texto | `especificacion_tecnica.valor` es VARCHAR. El servicio de compatibilidad interpreta `"65"` como número y `"ATX,mATX"` como lista antes de aplicar las reglas |
| D23 | Stock adelantado | La tabla `stock` se creó en esta etapa (prevista para inventario) para mostrar disponibilidad real. Movimientos, reservas, ingresos y alertas siguen en la etapa de inventario |
| D24 | Fotos | Se guardan en el volumen `orvex_fotos`. Se valida la firma real del archivo (no la extensión) y el nombre lo genera el servidor; solo se sirven nombres con ese formato |

## Verificación (2026-11-07)

- Migración V4 probada primero en un esquema temporal y luego aplicada a los datos reales: 30 productos y 69 especificaciones.
- Pruebas automáticas del catálogo: 33 (búsqueda, facetas, paginación, modo degradado, validación de especificaciones,
  transiciones de estado, fotos falsas y rutas fuera del directorio, permisos). 103 en total en el backend.
- En Docker, por el gateway: filtros, ficha con relacionados, cliente sin acceso al admin, alta en borrador,
  publicación bloqueada por especificaciones faltantes, error por campo en especificación numérica, foto falsa rechazada,
  foto real servida, transición inválida rechazada, SKU duplicado, auditoría de precio y estado, y catálogo funcionando
  con inventario detenido.
- En el navegador: filtros desde la URL, cambio de socket, ficha con compatibilidad y relacionados, "Armar una PC con
  este producto", lista y editor del panel de administración; sin desbordes a 375 px.
- Queda en la base local un producto de prueba inactivo (`RAM-TEST-…`) creado durante la verificación.
