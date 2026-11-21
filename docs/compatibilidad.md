# Armador y compatibilidad (M04)

Implementa HU-06, HU-07 y HU-08 (RF-06 a RF-11; RNF-06: las reglas se cambian sin tocar el código).

## Modelo de datos (`orvex_compatibilidad`)

| Tabla | Contenido |
|---|---|
| `regla_compatibilidad` | Nombre, las dos categorías que compara (`id_categoria_a`, `id_categoria_b`) y la condición en texto |
| `configuracion_pc` | PC guardada por un usuario: nombre, uso previsto (`juegos`, `edicion`, `oficina`), consumo estimado y precio total |
| `item_configuracion` | Productos de cada configuración (FK con borrado en cascada) |
| `resultado_compatibilidad` | Resultado de cada regla al guardar (`compatible`, `advertencia`, `incompatible`) y su mensaje |

`id_usuario` e `id_producto` se guardan sin FK entre esquemas (D3). Los id de categoría (1 Procesador … 8 Refrigeración)
son los mismos del catálogo. `resultado_compatibilidad` tiene FK `RESTRICT` hacia la regla: una regla que ya se usó no se
puede borrar (409), solo editar.

## Cómo se escribe una regla

```
a.<dato> <operador> b.<dato>        a.socket = b.socket
a.<dato> <operador> <número>        a.tdp_w <= 250
... [advertencia]                   a.frecuencia_mhz <= b.frecuencia_max_mhz [advertencia]
consumo(base=<W>) * <margen> <= b.<dato>    consumo(base=100) * 1.30 <= b.potencia_w
```

- `a` es el componente de la primera categoría y `b` el de la segunda; `<dato>` es una clave de especificación del catálogo.
- Operadores: `=`, `<`, `<=`, `>`, `>=`, `contiene` (la lista de `a` incluye el valor de `b`) y `en` (el valor de `a` está en la lista de `b`).
- Sin `[advertencia]`, una regla que no se cumple es **Incompatible** y bloquea la compra (RF-11); con `[advertencia]` solo avisa.
- Si a un producto le falta el dato, el resultado es **Advertencia** ("No se pudo verificar…"), nunca Compatible.
- La condición se interpreta **antes** de guardarla: una regla mal escrita devuelve 400 con el error en el campo `condicion` y
  nunca llega a la tabla. La gramática es cerrada (expresión regular), así que el texto no se evalúa como código.

## Reglas iniciales (10)

| Regla | Compara | Condición |
|---|---|---|
| Socket del procesador | Procesador → Placa madre | `a.socket = b.socket` |
| Tipo de memoria RAM | Memoria RAM → Placa madre | `a.tipo_ram = b.tipo_ram` |
| Frecuencia de la memoria | Memoria RAM → Placa madre | `a.frecuencia_mhz <= b.frecuencia_max_mhz [advertencia]` |
| Formato de placa y gabinete | Gabinete → Placa madre | `a.factores_forma contiene b.factor_forma` |
| Largo de la tarjeta gráfica | Tarjeta gráfica → Gabinete | `a.largo_mm <= b.max_largo_gpu_mm` |
| Altura del cooler | Refrigeración → Gabinete | `a.altura_mm <= b.max_altura_cooler_mm` |
| Cooler y socket | Refrigeración → Procesador | `a.sockets contiene b.socket` |
| Interfaz de almacenamiento | Almacenamiento → Placa madre | `a.interfaz en b.interfaces_almacenamiento` |
| Conectores de la fuente | Tarjeta gráfica → Fuente | `a.conectores_pcie <= b.conectores_pcie` |
| Consumo y fuente de poder | Procesador → Fuente (suma todo el armado) | `consumo(base=100) * 1.30 <= b.potencia_w` |

Las tres reglas nuevas de esta etapa (M.2/SATA, conectores PCIe y frecuencia de RAM) usan especificaciones que se agregaron
al catálogo en la migración `V6__specs_reglas_nuevas.sql` (placas: `interfaces_almacenamiento`, `frecuencia_max_mhz`;
gráficas y fuentes: `conectores_pcie`).

## API

| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| POST | `/api/armador/verificar` | Público | `{"productoIds":[…]}` → reporte, total y componentes |
| GET | `/api/configuraciones` | Con sesión | Configuraciones propias (resumen con estado) |
| POST | `/api/configuraciones` | Con sesión | `{"nombre","uso","productoIds"}`; verifica y guarda los resultados. Máximo 20 por usuario |
| GET | `/api/configuraciones/{id}` | Dueño | Productos y verificación actual (con las reglas y precios de hoy) |
| PUT | `/api/configuraciones/{id}` | Dueño | Reemplaza nombre, uso y productos; vuelve a verificar |
| DELETE | `/api/configuraciones/{id}` | Dueño | Borra la configuración y sus ítems y resultados |
| GET/POST | `/api/reglas` | Administrador | Lista / crea reglas |
| PUT/DELETE | `/api/reglas/{id}` | Administrador | Edita / elimina (409 si ya se usó en configuraciones guardadas) |

Una configuración de otro usuario responde **404**, no 403, para no revelar que existe.

## Pantallas

- **Armador** (`/armador`): botón "Guardar configuración" con nombre y uso previsto. Sin sesión lleva a ingresar y, al volver,
  recupera las piezas elegidas. `/armador?config=ID` abre una guardada; se puede guardar encima o como nueva.
- **Mi cuenta** → "Mis configuraciones": lista con estado, total y uso; abrir en el armador o eliminar con confirmación.
- **Administración** → "Reglas de compatibilidad" (`/admin/reglas`): tabla, crear/editar con ayuda de sintaxis y ejemplos,
  eliminar con confirmación.

## Decisiones

| # | Tema | Decisión |
|---|---|---|
| D28 | Mensajes de las reglas | Se generan a partir de la condición y de una tabla de etiquetas y unidades (`Etiquetas`), sin artículos ("El/La") para no equivocar el género con datos nuevos que defina el administrador |
| D29 | Caché de reglas | El servicio lee la tabla y arma el motor cada 30 s como máximo; al crear, editar o borrar una regla la caché se descarta. Con varias réplicas, el cambio tarda hasta 30 s en llegar a todas |
| D30 | Estado de una configuración | La lista muestra el peor resultado guardado (sin volver a verificar). Al abrirla se verifica de nuevo con las reglas y precios actuales; si una pieza ya no está publicada se avisa |
| D31 | Uso previsto | Opcional al guardar; lo usará el optimizador (Etapa 7) |
| D32 | Auditoría | Crear, editar y borrar reglas queda en el log `accion=regla_*` con el administrador responsable; pasa al servicio de auditoría en la Etapa 11 |

## Pruebas

- Motor (`compat-engine`): 17 pruebas, incluidas las reglas nuevas, condiciones mal escritas o con texto de más
  (`a.socket = b.socket; DROP TABLE x`) y constantes.
- Servicio: 10 pruebas web (MockMvc) y 5 contra MySQL 8 real (Testcontainers): reglas desde la tabla, editar una regla cambia el
  resultado, una regla mal escrita no se guarda, guardar/abrir/borrar una configuración propia y ajena, regla usada → 409.
- De punta a punta por el gateway con los datos de ejemplo: RAM DDR5 6000 en placa B760M (máx. 5600) → Advertencia;
  RX 7800 XT (2 conectores) con fuente de 450 W (1) → Incompatible; cliente → `/api/reglas` 403.
