export type Categoria =
  | "PROCESADOR"
  | "PLACA_MADRE"
  | "RAM"
  | "TARJETA_GRAFICA"
  | "ALMACENAMIENTO"
  | "FUENTE"
  | "GABINETE"
  | "REFRIGERACION";

export type Severidad = "COMPATIBLE" | "ADVERTENCIA" | "INCOMPATIBLE";

export interface Producto {
  id: number;
  sku: string;
  nombre: string;
  marca: string | null;
  precio: number;
  fotoUrl: string | null;
  categoria: Categoria;
  /** Pares nombre/valor del catálogo. Las listas vienen separadas por coma: "ATX,mATX". */
  specs: Record<string, string>;
  /** Unidades disponibles. null = inventario no respondió (disponibilidad por confirmar). */
  stock: number | null;
  enStock: boolean | null;
}

export type UsoPrevisto = "JUEGOS" | "EDICION" | "OFICINA";

export const USOS: { id: UsoPrevisto; etiqueta: string }[] = [
  { id: "JUEGOS", etiqueta: "Juegos" },
  { id: "EDICION", etiqueta: "Edición" },
  { id: "OFICINA", etiqueta: "Oficina" },
];

export interface ResumenConfiguracion {
  id: number;
  nombre: string;
  uso: UsoPrevisto | null;
  precioTotal: number;
  consumoEstimadoW: number | null;
  componentes: number;
  estado: Severidad;
  fechaCreacion: string;
}

export interface DetalleConfiguracion {
  configuracion: ResumenConfiguracion;
  productoIds: number[];
  /** null si algún producto ya no está publicado. */
  verificacionActual: RespuestaVerificacion | null;
}

/** Optimización (M05): cada hallazgo resta puntos de 100 y explica por qué. */
export type TipoHallazgo =
  | "COMPATIBILIDAD"
  | "BALANCE"
  | "USO"
  | "NUCLEOS"
  | "RAM"
  | "DOBLE_CANAL"
  | "ALMACENAMIENTO"
  | "GASTO"
  | "DATOS";

export interface Hallazgo {
  tipo: TipoHallazgo;
  /** Pieza que conviene cambiar o agregar (null si es general). */
  categoria: Categoria | null;
  puntos: number;
  titulo: string;
  explicacion: string;
}

export interface ProductoResumen {
  id: number;
  nombre: string;
  marca: string | null;
  precio: number;
  fotoUrl: string | null;
  stock: number | null;
}

export interface Alternativa {
  categoria: Categoria;
  /** null si la pieza falta y se propone agregarla. */
  reemplaza: ProductoResumen | null;
  producto: ProductoResumen;
  puntajeNuevo: number;
  diferenciaPrecio: number;
  motivo: string;
}

export interface AnalisisOptimizacion {
  puntaje: number;
  cuelloBotella: string | null;
  uso: UsoPrevisto;
  compatibilidad: Severidad;
  consumoEstimadoW: number;
  balance: number | null;
  recomendaciones: Hallazgo[];
  fortalezas: string[];
  buscoAlternativas: boolean;
  alternativas: Alternativa[];
  umbralAlternativas: number;
}

export async function analizarOptimizacion(productoIds: number[], uso: UsoPrevisto): Promise<AnalisisOptimizacion> {
  return pedir<AnalisisOptimizacion>("/api/optimizacion/analizar", { metodo: "POST", cuerpo: { productoIds, uso } });
}

export interface Facetas {
  marcas: string[];
  sockets: string[];
  tiposRam: string[];
  precioMaximo: number;
}

export interface Pagina<T> {
  elementos: T[];
  pagina: number;
  totalPaginas: number;
  total: number;
}

export interface Busqueda {
  resultados: Pagina<Producto>;
  facetas: Facetas;
  disponibilidadConfirmada: boolean;
}

export type Orden = "PRECIO_ASC" | "PRECIO_DESC" | "NOMBRE";

export interface ParametrosBusqueda {
  q?: string;
  categoria?: Categoria | "";
  marcas?: string[];
  precioMax?: number | null;
  socket?: string;
  tipoRam?: string;
  disponible?: boolean;
  orden?: Orden;
  pagina?: number;
  tamano?: number;
}

export interface ResultadoRegla {
  /** id en la tabla regla_compatibilidad; null para el aviso de cobertura. */
  reglaId: number | null;
  regla: string;
  severidad: Severidad;
  mensaje: string;
}

export interface Reporte {
  global: Severidad;
  resultados: ResultadoRegla[];
  consumoEstimadoW: number;
}

export interface RespuestaVerificacion {
  reporte: Reporte;
  precioTotal: number;
  componentes: Producto[];
}

export const CATEGORIAS: { id: Categoria; etiqueta: string }[] = [
  { id: "PROCESADOR", etiqueta: "Procesador" },
  { id: "PLACA_MADRE", etiqueta: "Placa madre" },
  { id: "RAM", etiqueta: "Memoria RAM" },
  { id: "TARJETA_GRAFICA", etiqueta: "Tarjeta gráfica" },
  { id: "ALMACENAMIENTO", etiqueta: "Almacenamiento" },
  { id: "REFRIGERACION", etiqueta: "Refrigeración" },
  { id: "GABINETE", etiqueta: "Gabinete" },
  { id: "FUENTE", etiqueta: "Fuente de poder" },
];

export const etiquetaCategoria = (c: Categoria) => CATEGORIAS.find((x) => x.id === c)?.etiqueta ?? c;

/** Los servicios guardan fechas en UTC sin zona */
export const fechaUtc = (iso: string) => new Date(/[zZ]|[+-]\d\d:\d\d$/.test(iso) ? iso : `${iso}Z`);

export const esCategoria = (v: string | null): v is Categoria => CATEGORIAS.some((c) => c.id === v);

export const bs = (n: number) =>
  `Bs ${n.toLocaleString("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

/** Por defecto vacío: las peticiones van a /api del mismo dominio y Next.js las reenvía al gateway */
export const BASE = process.env.NEXT_PUBLIC_API_URL ?? "";

/** Error de la API con el mensaje general y, si los hay, los mensajes de cada campo del formulario. */
export class ErrorApi extends Error {
  constructor(
    mensaje: string,
    readonly estado: number,
    readonly campos: Record<string, string> = {},
  ) {
    super(mensaje);
  }
}

export async function leer<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let mensaje = `Error ${res.status}`;
    let campos: Record<string, string> = {};
    try {
      const cuerpo = await res.json();
      if (cuerpo?.error) mensaje = cuerpo.error;
      if (cuerpo?.campos) campos = cuerpo.campos;
    } catch {
      /* respuesta sin JSON */
    }
    throw new ErrorApi(mensaje, res.status, campos);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

/** Petición a la API a través del gateway. Con token, va con la sesión del usuario. */
export async function pedir<T>(
  ruta: string,
  { metodo = "GET", cuerpo, token }: { metodo?: string; cuerpo?: unknown; token?: string | null } = {},
): Promise<T> {
  const headers: Record<string, string> = {};
  if (cuerpo !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;
  let res: Response;
  try {
    res = await fetch(`${BASE}${ruta}`, {
      method: metodo,
      headers,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    });
  } catch {
    throw new ErrorApi("No hay conexión con el servidor. Revisa tu conexión e inténtalo de nuevo.", 0);
  }
  return leer<T>(res);
}

export async function listarProductos(): Promise<Producto[]> {
  return pedir<Producto[]>("/api/productos");
}

export async function buscarProductos(p: ParametrosBusqueda): Promise<Busqueda> {
  const q = new URLSearchParams();
  if (p.q) q.set("q", p.q);
  if (p.categoria) q.set("categoria", p.categoria);
  p.marcas?.forEach((m) => q.append("marca", m));
  if (p.precioMax != null) q.set("precioMax", String(p.precioMax));
  if (p.socket) q.set("socket", p.socket);
  if (p.tipoRam) q.set("tipoRam", p.tipoRam);
  if (p.disponible) q.set("disponible", "true");
  q.set("orden", p.orden ?? "PRECIO_ASC");
  q.set("pagina", String(p.pagina ?? 0));
  q.set("tamano", String(p.tamano ?? 12));
  return pedir<Busqueda>(`/api/productos/buscar?${q}`);
}

export async function detalleProducto(id: number): Promise<{ producto: Producto; relacionados: Producto[] }> {
  return pedir(`/api/productos/${id}`);
}

/** Las fotos se sirven por el gateway; la URL guardada es relativa a la API. */
export const urlFoto = (fotoUrl: string | null) => (fotoUrl ? `${BASE}${fotoUrl}` : null);

export async function verificarArmado(productoIds: number[]): Promise<RespuestaVerificacion> {
  return pedir<RespuestaVerificacion>("/api/armador/verificar", { metodo: "POST", cuerpo: { productoIds } });
}
