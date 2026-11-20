"use client";

import { CATEGORIAS, type Categoria, type Facetas } from "@/lib/api";
import { PriceDisplay } from "./PriceDisplay";

export interface Filtros {
  q: string;
  categoria: Categoria | "";
  marcas: string[];
  precioMax: number | null;
  socket: string;
  tipoRam: string;
  soloStock: boolean;
}

export const FILTROS_VACIOS: Filtros = {
  q: "",
  categoria: "",
  marcas: [],
  precioMax: null,
  socket: "",
  tipoRam: "",
  soloStock: false,
};

/** Filtros del catálogo */
export function FilterPanel({
  filtros,
  onCambiar,
  facetas,
  paso = 50,
}: {
  filtros: Filtros;
  onCambiar: (f: Filtros) => void;
  facetas: Facetas | null;
  paso?: number;
}) {
  const set = <K extends keyof Filtros>(k: K, v: Filtros[K]) => onCambiar({ ...filtros, [k]: v });
  const tope = Math.max(paso, Math.ceil((facetas?.precioMaximo ?? 0) / paso) * paso);
  const activos = JSON.stringify({ ...filtros, q: filtros.q.trim() }) !== JSON.stringify(FILTROS_VACIOS);

  return (
    <aside className="tarjeta filtros" aria-label="Filtros del catálogo">
      <h3>Buscar</h3>
      <input
        className="entrada"
        type="search"
        value={filtros.q}
        onChange={(e) => set("q", e.target.value)}
        placeholder="Nombre, marca o SKU"
        aria-label="Buscar por nombre, marca o SKU"
      />

      <h3>Categoría</h3>
      <select
        className="entrada"
        value={filtros.categoria}
        onChange={(e) => onCambiar({ ...filtros, categoria: e.target.value as Categoria | "", marcas: [], socket: "", tipoRam: "" })}
        aria-label="Categoría"
      >
        <option value="">Todas las categorías</option>
        {CATEGORIAS.map((c) => (
          <option key={c.id} value={c.id}>
            {c.etiqueta}
          </option>
        ))}
      </select>

      {facetas && facetas.marcas.length > 0 && (
        <>
          <h3>Marca</h3>
          <div role="group" aria-label="Marcas">
            {facetas.marcas.map((m) => (
              <label className="casilla" key={m}>
                <input
                  type="checkbox"
                  checked={filtros.marcas.includes(m)}
                  onChange={(e) =>
                    set("marcas", e.target.checked ? [...filtros.marcas, m] : filtros.marcas.filter((x) => x !== m))
                  }
                />
                {m}
              </label>
            ))}
          </div>
        </>
      )}

      {facetas && facetas.sockets.length > 0 && (
        <>
          <h3>Socket</h3>
          <select className="entrada" value={filtros.socket} onChange={(e) => set("socket", e.target.value)} aria-label="Socket">
            <option value="">Cualquier socket</option>
            {facetas.sockets.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </>
      )}

      {facetas && facetas.tiposRam.length > 0 && (
        <>
          <h3>Tipo de memoria</h3>
          <select className="entrada" value={filtros.tipoRam} onChange={(e) => set("tipoRam", e.target.value)} aria-label="Tipo de memoria">
            <option value="">Cualquier tipo</option>
            {facetas.tiposRam.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </>
      )}

      <h3>Precio máximo</h3>
      <input
        type="range"
        min={0}
        max={tope}
        step={paso}
        value={filtros.precioMax ?? tope}
        onChange={(e) => {
          const v = Number(e.target.value);
          set("precioMax", v >= tope ? null : v);
        }}
        aria-label="Precio máximo"
      />
      <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
        Hasta <PriceDisplay monto={filtros.precioMax ?? tope} tamano="sm" />
      </p>

      <h3>Disponibilidad</h3>
      <label className="casilla">
        <input type="checkbox" checked={filtros.soloStock} onChange={(e) => set("soloStock", e.target.checked)} />
        Solo productos en stock
      </label>

      {activos && (
        <button
          className="btn btn-fantasma btn-sm btn-bloque"
          style={{ marginTop: "var(--s-4)" }}
          onClick={() => onCambiar(FILTROS_VACIOS)}
        >
          Limpiar filtros
        </button>
      )}
    </aside>
  );
}
