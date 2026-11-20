import type { Categoria } from "./api";

/** Contratos de la administración del catálogo (servicio de catálogo, /api/catalogo/admin). */
export type EstadoProducto = "BORRADOR" | "PUBLICADO" | "INACTIVO";

export interface ProductoAdmin {
  id: number;
  sku: string;
  nombre: string;
  marca: string | null;
  precio: number;
  fotoUrl: string | null;
  categoria: Categoria;
  estado: EstadoProducto;
  specs: Record<string, string>;
  /** Especificaciones requeridas que faltan para poder publicarlo. */
  faltantes: string[];
}

export interface SpecConocida {
  clave: string;
  etiqueta: string;
  tipo: "TEXTO" | "NUMERO" | "LISTA";
  unidad: string | null;
  requerida: boolean;
}

export interface CategoriaSpecs {
  categoria: Categoria;
  nombre: string;
  especificaciones: SpecConocida[];
}

export const ESTADO: Record<EstadoProducto, { texto: string; tono: "info" | "exito" | "neutro" }> = {
  BORRADOR: { texto: "Borrador", tono: "info" },
  PUBLICADO: { texto: "Publicado", tono: "exito" },
  INACTIVO: { texto: "Inactivo", tono: "neutro" },
};

/** Transiciones válidas (diagrama de estados del producto). */
export const SIGUIENTES: Record<EstadoProducto, { estado: EstadoProducto; accion: string }[]> = {
  BORRADOR: [{ estado: "PUBLICADO", accion: "Publicar" }],
  PUBLICADO: [{ estado: "INACTIVO", accion: "Desactivar" }],
  INACTIVO: [{ estado: "PUBLICADO", accion: "Volver a publicar" }],
};
