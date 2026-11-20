"use client";

import { Check } from "lucide-react";
import type { Producto } from "@/lib/api";
import { specsLegibles } from "@/lib/specs";
import { StockBadge } from "./Badges";
import { ImagenProducto } from "./ImagenProducto";
import { PriceDisplay } from "./PriceDisplay";

/** Lista de opciones de una categoría del armador */
export function ComponentSelector({
  productos,
  seleccionado,
  onElegir,
  deshabilitado = false,
}: {
  productos: Producto[];
  seleccionado?: number;
  onElegir: (id: number | undefined) => void;
  deshabilitado?: boolean;
}) {
  return (
    <div className="opciones entrar" role="list">
      {productos.map((p) => {
        const elegido = p.id === seleccionado;
        return (
          <button
            key={p.id}
            role="listitem"
            className="opcion"
            aria-pressed={elegido}
            disabled={deshabilitado || (p.enStock === false && !elegido)}
            onClick={() => onElegir(elegido ? undefined : p.id)}
          >
            <div className="opcion-imagen">
              {elegido ? <Check size={26} /> : <ImagenProducto producto={p} tamano="mini" />}
            </div>
            <div className="pila" style={{ gap: 6 }}>
              <div>
                {p.marca && <span className="prod-marca">{p.marca}</span>}
                <h3>{p.nombre}</h3>
              </div>
              <div className="specs">
                {specsLegibles(p.specs, 4).map((s) => (
                  <span className="spec" key={s.clave}>
                    {s.nombre} {s.valor}
                  </span>
                ))}
              </div>
            </div>
            <div className="pila opcion-precio" style={{ gap: 6, alignItems: "end" }}>
              <PriceDisplay monto={p.precio} tamano="sm" />
              <StockBadge stock={p.stock} enStock={p.enStock} />
            </div>
          </button>
        );
      })}
    </div>
  );
}
