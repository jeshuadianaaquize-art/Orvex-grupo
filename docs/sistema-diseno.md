# Sistema de diseño de Orvex

Guía viva con todos los componentes funcionando: **http://localhost:3000/diseno** (sus datos son de ejemplo).

## Principios

- **Tienda clara, herramientas oscuras.** Inicio, catálogo y carrito usan fondo claro para destacar los productos.
  El configurador (y en próximas etapas los paneles y el POS) usan `.tema-oscuro`, que redefine los mismos tokens.
- **El color nunca es la única señal.** Compatible, advertencia e incompatible llevan icono y texto.
- **Nada decorativo que parezca funcional.** Solo se muestran enlaces y botones que hacen algo real.
- **Sin datos inventados.** Cifras, precios "desde" y opciones destacadas salen de la API.
- **Movimiento moderado.** Entradas escalonadas, hover y transiciones de 120–360 ms; con
  `prefers-reduced-motion` las animaciones se desactivan.

## Tokens (`frontend/app/globals.css`)

La paleta es la **propuesta** del prompt de diseño, no un requisito del informe. Cambiarla es editar `:root`.

| Grupo | Tokens |
|---|---|
| Paleta | `--noche-950/900/800`, `--gris-50…700`, `--azul-600/500`, `--cian-500`, `--verde-600`, `--ambar-500`, `--rojo-600` |
| Semánticos | `--c-fondo`, `--c-superficie(-2)`, `--c-borde(-fuerte)`, `--c-texto(-2)`, `--c-primario(-hover/-suave)`, `--c-exito/aviso/error(-suave)` |
| Tipografía | Space Grotesk (títulos), Inter (texto), JetBrains Mono (etiquetas y datos); escala `--t-xs` a `--t-3xl` |
| Espaciado | Base 4 px: `--s-1` (4) a `--s-16` (64) |
| Radios | `--r-sm` 8 · `--r-md` 12 · `--r-lg` 16 · `--r-xl` 22 · `--r-pill` |
| Sombras | `--sombra-1` (reposo), `--sombra-2` (hover), `--sombra-3` (modales y tarjetas elevadas) |
| Movimiento | `--dur-1/2/3` (120/220/360 ms), `--ease` |

Botones: `.btn` + `btn-primario | btn-secundario | btn-fantasma | btn-peligro`, tamaños `btn-sm | btn-lg`, `btn-icono`, `btn-bloque`.

## Componentes (`frontend/components/`)

| Componente | Archivo | Uso |
|---|---|---|
| Navbar | `Navbar.tsx` | Logo, navegación, búsqueda, carrito con contador animado; menú móvil |
| Sidebar | `Sidebar.tsx` | Navegación de paneles (cliente, admin) |
| Footer | `Footer.tsx` | Pie con categorías y enlaces reales |
| ProductCard | `ProductCard.tsx` | Producto del catálogo con specs, precio, stock y "Agregar" |
| CategoryCard | `CategoryCard.tsx` | Acceso a categoría con cantidad y precio "desde" |
| SearchBar | `SearchBar.tsx` | Búsqueda global → `/tienda?q=` |
| FilterPanel | `FilterPanel.tsx` | Texto, categoría, marca, precio y disponibilidad (RF-04) |
| PriceDisplay | `PriceDisplay.tsx` | Importes en Bs con formato `es-BO` |
| StockBadge · CompatibilityBadge · StatusBadge | `Badges.tsx` | Estados con icono/punto + texto |
| Modal · ConfirmDialog | `Modal.tsx` | Accesibles: Escape, clic fuera, foco atrapado y devuelto |
| DataTable | `DataTable.tsx` | Tablas con scroll horizontal propio en móvil |
| Pagination | `Pagination.tsx` | Páginas con elipsis |
| FormField | `FormField.tsx` | Etiqueta + control + ayuda/error con `aria-describedby` |
| ToastNotification | `Toast.tsx` | `useToast()` para avisos breves |
| LoadingSkeleton · EmptyState · ErrorState | `Estados.tsx` | Estados de carga, vacío y error con reintento |
| ChartCard | `ChartCard.tsx` | Barras simples; sin datos muestra estado vacío |
| CartItem | `CartItem.tsx` | Línea del carrito con cantidad y subtotal |
| ComponentSelector | `ComponentSelector.tsx` | Opciones de una categoría del armador |
| PcSummary | `PcSummary.tsx` | Resumen del armado: total, consumo, reglas, bloqueo de compra |

Iconos: `lucide-react` para la interfaz; `components/iconos.tsx` tiene el logo, un icono propio de tarjeta gráfica
(lucide no trae uno) y `IconoCategoria`.

## Decisiones de esta etapa

| Tema | Decisión |
|---|---|
| Bootstrap | Eliminado. Su estética chocaba con la identidad propia y obligaba a sobrescribir estilos |
| Menú | "Computadoras", "Ofertas" y "Mi cuenta" no aparecen todavía: no hay datos ni pantallas detrás (D6, etapa 3) |
| Inicio | "Más vendidos", "Ofertas" y "Contacto" se omiten hasta que existan ventas, precios de oferta y datos de contacto reales |
| "Opciones más accesibles" | Reemplaza a "destacados": es el producto más barato con stock de 4 categorías, calculado de la API |
| Fotos | Ilustraciones por categoría marcadas "Imagen ilustrativa" hasta que el admin pueda subir fotos (D10) |
| Fuentes | Autoalojadas con paquetes `@fontsource` y `next/font/local`. Antes se bajaban de Google Fonts al compilar y eso rompía el build en Docker y en la CI |

## Verificación (2026-11-07)

- `npm run build` sin errores de tipos ni de estilos.
- En el navegador: inicio, catálogo, configurador, carrito y `/diseno` sin errores de consola.
- A 375 px de ancho ninguna de las 5 páginas tiene scroll horizontal.
- Configurador a 1366 px: 3 columnas; con un armado incompatible explica la regla y bloquea "Agregar al carrito".
- Carrito: el diálogo de vaciar se cierra con Escape sin borrar nada.
