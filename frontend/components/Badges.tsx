import { CircleCheck, CircleX, TriangleAlert } from "lucide-react";
import type { Severidad } from "@/lib/api";

/** Umbral para avisar "últimas unidades". Es un valor de presentación, no una regla de inventario. */
export const POCAS_UNIDADES = 3;

/** Disponibilidad según el servicio de inventario */
export function StockBadge({ stock, enStock }: { stock?: number | null; enStock: boolean | null }) {
  if (enStock === null) {
    return (
      <span className="insignia" title="No pudimos consultar el inventario en este momento">
        <span className="punto" /> Disponibilidad por confirmar
      </span>
    );
  }
  if (!enStock) {
    return (
      <span className="insignia insignia-error">
        <span className="punto" /> Sin stock
      </span>
    );
  }
  if (stock != null && stock <= POCAS_UNIDADES) {
    return (
      <span className="insignia insignia-aviso">
        <span className="punto" /> {stock === 1 ? "Última unidad" : `Últimas ${stock} unidades`}
      </span>
    );
  }
  return (
    <span className="insignia insignia-exito">
      <span className="punto" /> En stock
    </span>
  );
}

const COMPAT: Record<Severidad, { clase: string; texto: string; Icono: typeof CircleCheck }> = {
  COMPATIBLE: { clase: "insignia-exito", texto: "Compatible", Icono: CircleCheck },
  ADVERTENCIA: { clase: "insignia-aviso", texto: "Advertencia", Icono: TriangleAlert },
  INCOMPATIBLE: { clase: "insignia-error", texto: "Incompatible", Icono: CircleX },
};

/** Resultado de una regla de compatibilidad (RF-07): icono + texto, nunca solo color. */
export function CompatibilityBadge({ severidad }: { severidad: Severidad }) {
  const { clase, texto, Icono } = COMPAT[severidad];
  return (
    <span className={`insignia ${clase}`}>
      <Icono aria-hidden="true" /> {texto}
    </span>
  );
}

export type Tono = "exito" | "aviso" | "error" | "info" | "neutro";

/** Estado genérico (pedidos, pagos, servicios…) */
export function StatusBadge({ tono = "neutro", children }: { tono?: Tono; children: React.ReactNode }) {
  return (
    <span className={`insignia ${tono === "neutro" ? "" : `insignia-${tono}`}`}>
      <span className="punto" />
      {children}
    </span>
  );
}
