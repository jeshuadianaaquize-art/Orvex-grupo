# Optimización y alternativas (M05)

Implementa HU-09 y HU-10 (RF-09 optimización, RF-10 alternativas). Servicio `optimizacion` (puerto 8104, esquema
`orvex_optimizacion`).

> **Los umbrales de esta página son una PROPUESTA.** El informe pide balance CPU/GPU, cuello de botella, RAM y doble
> canal, adecuación al uso y puntaje 0–100, pero no fija fórmulas ni umbrales (contexto, sección 13). Están en
> `servicios/optimizacion/src/main/resources/application.yml` (`orvex.optimizacion.*`) y se cambian sin tocar el
> código. El equipo debe revisarlos.

## Cómo se calcula el puntaje

Se parte de **100** y cada hallazgo resta puntos. Cada hallazgo dice **por qué** resta y qué pieza conviene cambiar.

| Hallazgo | Resta | Cuándo |
|---|---|---|
| Incompatibilidad | 25 por regla | Una regla de compatibilidad da Incompatible. Además, mientras haya alguna, el puntaje no pasa de **40** |
| Advertencia de compatibilidad | 5 por regla | Hasta un máximo de 50 entre ambas |
| Balance procesador/gráfica | hasta 30 | Balance = rendimiento de la gráfica ÷ rendimiento del procesador, fuera del rango del uso. Resta el % de desvío |
| Falta tarjeta gráfica | 30 juegos · 15 edición | Sin gráfica dedicada |
| Gráfica de más | 10 | Oficina con una gráfica de rendimiento mayor a 40 |
| Pocos núcleos | 10 | Menos de 6 (juegos), 8 (edición) o 4 (oficina) |
| Poca RAM | 10 a 20 | Menos de 16 GB (juegos), 32 GB (edición) u 8 GB (oficina); proporcional a lo que falta |
| Un solo canal | 8 | El kit de RAM es de un solo módulo |
| Disco lento | 8 | El disco principal no es M.2 (o no hay disco) |

| Uso | Balance gráfica/procesador | RAM mínima | Núcleos |
|---|---|---|---|
| Juegos | 0,9 – 1,6 | 16 GB | 6 |
| Edición | 0,7 – 1,3 | 32 GB | 8 |
| Oficina | no se evalúa | 8 GB | 4 |

**Cuello de botella** (columna `cuello_botella`): el título del hallazgo que más resta. Si a una pieza le falta un dato
(por ejemplo el índice de rendimiento), el análisis lo avisa y **no resta ni inventa** ese punto.

### Índice de rendimiento

Para comparar procesador y gráfica se usa la especificación `rendimiento`: un índice relativo de 1 a 100 entre los
productos del catálogo. En los datos de demostración (`V7__specs_optimizador.sql`) es una estimación a partir de
comparativas públicas. El administrador lo corrige en la ficha de cada producto. La migración también agrega núcleos,
hilos, memoria de video y módulos de cada kit de RAM.

## Alternativas (RF-10)

Se buscan cuando hay alguna incompatibilidad o el puntaje es **menor a 70**. Se prueban las piezas de las categorías
señaladas: las de las reglas incompatibles y la de cada recomendación. Una alternativa se propone solo si:

1. **tiene stock** (si el inventario no responde, no se propone nada de esa categoría);
2. reemplaza **una sola pieza**, sin rehacer la PC;
3. **vuelve a pasar todas las reglas**: el mismo motor (`compat-engine`) con las reglas vigentes del servicio de
   compatibilidad, sin agregar ninguna incompatibilidad nueva;
4. **resuelve** alguna incompatibilidad o **sube el puntaje**.

Se muestran hasta 3 por categoría, de mayor a menor puntaje y, a igual puntaje, la más barata. Cada una dice qué
resuelve, cómo cambia el puntaje y la diferencia de precio. En el armador, "Reemplazar" cambia la pieza y vuelve a
analizar.

## Modelo de datos (`orvex_optimizacion`)

| Tabla | Contenido |
|---|---|
| `analisis_optimizacion` | Un análisis por configuración (`id_configuracion` UNIQUE): puntaje (CHECK 0–100), cuello de botella, recomendaciones (JSON con su explicación) y fecha |
| `alternativa_sugerida` | Productos propuestos en ese análisis; clave compuesta (`id_analisis`, `id_producto`) |

`id_configuracion` e `id_producto` no tienen FK: son de otros esquemas (D3). Un nuevo análisis de la misma configuración
reemplaza al anterior.

## API

| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| POST | `/api/optimizacion/analizar` | Público | `{"productoIds":[…],"uso":"JUEGOS"}` → puntaje, recomendaciones, lo que está bien y alternativas. No guarda nada |
| POST | `/api/optimizacion/configuraciones/{id}` | Dueño | Analiza una configuración guardada y guarda el resultado. Usa su uso previsto, o el del cuerpo `{"uso":…}` si no tiene |
| GET | `/api/optimizacion/configuraciones/{id}` | Dueño | Último análisis guardado (404 si aún no tiene) |

Internamente, optimización consulta `GET /api/productos` del catálogo (precio, especificaciones y stock) y la API
interna de compatibilidad: `GET /internal/reglas` (con caché de 30 s) y `GET /internal/configuraciones/{id}?usuario=`.
Las rutas `/internal` no pasan por el gateway.

## Decisiones

| # | Tema | Decisión |
|---|---|---|
| D33 | Umbrales sin fuente en el informe | Valores propuestos en `application.yml`, documentados aquí. Una prueba verifica que coincidan con `ParametrosOptimizacion.propuestos()` |
| D34 | Rendimiento de procesador y gráfica | Índice relativo 1–100 como especificación del catálogo (editable), no una tabla de benchmarks |
| D35 | Puntaje con incompatibilidades | Tope de 40 para que una PC que no se puede comprar no aparezca como "aceptable" |
| D36 | Análisis guardado | Solo cuando se analiza la configuración abierta sin cambios. Al cambiar una pieza, el análisis es en vivo y no se guarda hasta guardar la configuración |
| D37 | Interpretación de especificaciones | `Especificaciones` pasa al módulo `compat-engine` para que compatibilidad y optimización lean igual los datos del catálogo |

## Pruebas

- `AnalizadorTest` (9): armado equilibrado = 100, gráfica que limita, procesador que limita, edición con poca RAM y núcleos,
  gasto en oficina, juegos sin gráfica, incompatibilidades con tope de 40, dato faltante sin inventar, sin procesador.
- `AlternativasTest` (4): fuentes que resuelven los conectores, mejoras en stock que siguen siendo compatibles (descarta
  productos sin stock y procesadores de otro socket), armado bien puntuado sin alternativas, productos inválidos.
- `OptimizacionMysqlTest` (4, MySQL 8 real): umbrales de `application.yml` = propuestos, el análisis en vivo no guarda,
  validación y sesión, análisis guardado una sola vez con sus alternativas y 404 para otro usuario.
- De punta a punta por el gateway con los datos de demostración, y en el navegador (también a 375 px).
