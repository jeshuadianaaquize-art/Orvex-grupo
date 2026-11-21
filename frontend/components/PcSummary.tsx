"use client";

import { CircleCheck, CircleX, Loader2, ShoppingCart, TriangleAlert, Zap } from "lucide-react";
import { CATEGORIAS, type Categoria, type Producto, type Reporte } from "@/lib/api";
import { CompatibilityBadge } from "./Badges";
import { IconoCategoria } from "./iconos";
import { PriceDisplay } from "./PriceDisplay";

const GLOBAL = {
  COMPATIBLE: { Icono: CircleCheck, clase: "alerta-exito", texto: "Todo es compatible" },
  ADVERTENCIA: { Icono: TriangleAlert, clase: "alerta-aviso", texto: "Compatible con advertencias" },
  INCOMPATIBLE: { Icono: CircleX, clase: "alerta-error", texto: "Hay piezas incompatibles" },
} as const;

export function PcSummary({
  seleccion,
  reporte,
  verificando,
  desactualizado,
  onVerificar,
  onAgregar,
  agregado,
  extra,
}: {
  seleccion: Partial<Record<Categoria, Producto>>;
  reporte: Reporte | null;
  verificando: boolean;
  desactualizado: boolean;
  onVerificar: () => void;
  onAgregar: () => void;
  agregado: boolean;
  /** Acciones adicionales al pie (por ejemplo, guardar la configuración). */
  extra?: React.ReactNode;
}) {
  const elegidos = Object.values(seleccion).filter(Boolean) as Producto[];
  const total = elegidos.reduce((s, p) => s + p.precio, 0);
  const g = reporte ? GLOBAL[reporte.global] : null;

  return (
    <aside className="tarjeta resumen-pc" aria-label="Resumen de tu PC">
      <div className="tarjeta-cuerpo pila">
        <div className="fila" style={{ justifyContent: "space-between" }}>
          <h2 style={{ fontSize: "var(--t-lg)" }}>Tu PC</h2>
          <span className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
            {elegidos.length} de {CATEGORIAS.length}
          </span>
        </div>
        <div className="progreso" aria-hidden="true">
          <span style={{ width: `${(elegidos.length / CATEGORIAS.length) * 100}%` }} />
        </div>

        <div>
          {CATEGORIAS.map((c) => {
            const p = seleccion[c.id];
            return (
              <div key={c.id} className={`linea ${p ? "" : "pendiente"}`}>
                <IconoCategoria categoria={c.id} size={18} />
                <span style={{ minWidth: 0, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {p ? p.nombre : `${c.etiqueta} · pendiente`}
                </span>
                {p ? <PriceDisplay monto={p.precio} tamano="sm" /> : <span>—</span>}
              </div>
            );
          })}
        </div>

        <div className="fila" style={{ justifyContent: "space-between" }}>
          <span className="texto-2">Total</span>
          <PriceDisplay monto={total} tamano="lg" />
        </div>
        {reporte && !desactualizado && reporte.consumoEstimadoW > 0 && (
          <div className="fila texto-2" style={{ fontSize: "var(--t-sm)" }}>
            <Zap size={16} /> Consumo estimado: <strong style={{ color: "var(--c-texto)" }}>{reporte.consumoEstimadoW} W</strong>
          </div>
        )}

        <button
          className="btn btn-primario btn-lg btn-bloque"
          disabled={elegidos.length < 2 || verificando}
          onClick={onVerificar}
        >
          {verificando && <Loader2 size={18} className="girar" />}
          {verificando ? "Verificando…" : reporte && !desactualizado ? "Verificar de nuevo" : "Verificar compatibilidad"}
        </button>
        {elegidos.length < 2 && (
          <p className="texto-2" style={{ fontSize: "var(--t-xs)", textAlign: "center" }}>
            Elige al menos dos componentes para verificar.
          </p>
        )}
        {desactualizado && reporte && (
          <p className="texto-2" style={{ fontSize: "var(--t-xs)", textAlign: "center" }}>
            Cambiaste piezas: vuelve a verificar.
          </p>
        )}

        {reporte && !desactualizado && g && (
          <>
            <div className={`estado-global alerta ${g.clase}`} role="status">
              <g.Icono aria-hidden="true" />
              {g.texto}
            </div>
            <div>
              {reporte.resultados.map((r) => (
                <div className="regla" key={r.regla}>
                  <CompatibilityBadge severidad={r.severidad} />
                  <div>
                    <strong style={{ fontSize: "var(--t-sm)" }}>{r.regla}</strong>
                    <p>{r.mensaje}</p>
                  </div>
                </div>
              ))}
            </div>
            <button
              className="btn btn-secundario btn-bloque"
              disabled={reporte.global === "INCOMPATIBLE" || agregado}
              onClick={onAgregar}
            >
              <ShoppingCart size={16} />
              {agregado ? "Armado agregado al carrito" : "Agregar armado al carrito"}
            </button>
            {reporte.global === "INCOMPATIBLE" && (
              <p className="texto-2" style={{ fontSize: "var(--t-xs)", textAlign: "center" }}>
                Corrige las incompatibilidades para poder comprar este armado.
              </p>
            )}
          </>
        )}
        {extra}
      </div>
    </aside>
  );
}
