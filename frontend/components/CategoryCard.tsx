import { ChevronRight } from "lucide-react";
import Link from "next/link";
import type { Categoria } from "@/lib/api";
import { IconoCategoria } from "./iconos";
import { imagenCategoria } from "./ImagenProducto";
import { PriceDisplay } from "./PriceDisplay";

/** Acceso a una categoría del catálogo. Cantidad y precio "desde" se calculan con los datos reales. */
export function CategoryCard({
  categoria,
  nombre,
  descripcion,
  cantidad,
  desde,
}: {
  categoria: Categoria;
  nombre: string;
  descripcion: string;
  cantidad?: number;
  desde?: number;
}) {
  return (
    <Link className="cat-card" href={`/tienda?cat=${categoria}`}>
      <div className="cat-card-arte" aria-hidden="true">
        {/* eslint-disable-next-line @next/next/no-img-element -- SVG estático y animado */}
        <img src={imagenCategoria(categoria)} alt="" loading="lazy" />
        <span className="cat-card-icono">
          <IconoCategoria categoria={categoria} size={20} strokeWidth={1.8} />
        </span>
      </div>
      <div>
        <h3>{nombre}</h3>
        <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
          {descripcion}
        </p>
      </div>
      <div className="pie">
        <span>
          {cantidad === undefined ? " " : `${cantidad} producto${cantidad === 1 ? "" : "s"}`}
          {desde !== undefined && (
            <>
              {" · "}
              <PriceDisplay monto={desde} tamano="sm" prefijo="desde" />
            </>
          )}
        </span>
        <ChevronRight size={18} />
      </div>
    </Link>
  );
}
