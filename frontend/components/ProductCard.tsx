"use client";

import { Check, ShoppingCart } from "lucide-react";
import Link from "next/link";
import { etiquetaCategoria, type Producto } from "@/lib/api";
import { specsLegibles } from "@/lib/specs";
import { StockBadge } from "./Badges";
import { ImagenProducto } from "./ImagenProducto";
import { PriceDisplay } from "./PriceDisplay";

/** Tarjeta de producto del catálogo */
export function ProductCard({
  producto,
  onAgregar,
  agregado = false,
}: {
  producto: Producto;
  onAgregar: (p: Producto) => void;
  agregado?: boolean;
}) {
  const specs = specsLegibles(producto.specs, 3);
  const ficha = `/producto/${producto.id}`;
  return (
    <article className="prod-card">
      <Link href={ficha} className="prod-imagen" tabIndex={-1} aria-hidden="true">
        <span className="insignia">{etiquetaCategoria(producto.categoria)}</span>
        <ImagenProducto producto={producto} />
      </Link>
      <div className="prod-cuerpo">
        {producto.marca && <span className="prod-marca">{producto.marca}</span>}
        <Link href={ficha} className="prod-titulo">
          <h3>{producto.nombre}</h3>
        </Link>
        {specs.length > 0 && (
          <div className="specs">
            {specs.map((s) => (
              <span className="spec" key={s.clave} title={s.nombre}>
                {s.nombre} {s.valor}
              </span>
            ))}
          </div>
        )}
        <div className="prod-pie">
          <div className="pila" style={{ gap: 6 }}>
            <PriceDisplay monto={producto.precio} />
            <StockBadge stock={producto.stock} enStock={producto.enStock} />
          </div>
          <button
            className={`btn btn-sm ${agregado ? "btn-secundario" : "btn-primario"}`}
            disabled={producto.enStock === false}
            onClick={() => onAgregar(producto)}
            aria-label={`Agregar ${producto.nombre} al carrito`}
          >
            {agregado ? <Check size={16} /> : <ShoppingCart size={16} />}
            {agregado ? "Agregado" : "Agregar"}
          </button>
        </div>
      </div>
    </article>
  );
}
