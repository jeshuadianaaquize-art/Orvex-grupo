"use client";

import { ArrowRight, CircleCheck, Gauge, Info, Loader2, Replace, TriangleAlert } from "lucide-react";
import {
  USOS,
  bs,
  etiquetaCategoria,
  type Alternativa,
  type AnalisisOptimizacion,
  type Categoria,
  type UsoPrevisto,
} from "@/lib/api";
import { IconoCategoria } from "./iconos";
import { PriceDisplay } from "./PriceDisplay";

/** Color del puntaje: verde desde 85, ámbar desde el umbral de alternativas, rojo por debajo. */
function tono(puntaje: number, umbral: number) {
  if (puntaje >= 85) return "exito";
  if (puntaje >= umbral) return "aviso";
  return "error";
}

function Puntaje({ valor, umbral }: { valor: number; umbral: number }) {
  const t = tono(valor, umbral);
  return (
    <div
      className="puntaje"
      role="img"
      aria-label={`Puntaje de optimización: ${valor} de 100`}
      style={{ "--valor": valor, "--color": `var(--c-${t})` } as React.CSSProperties}
    >
      <span>
        <strong>{valor}</strong>
        <small>/100</small>
      </span>
    </div>
  );
}

/** Análisis de optimización del armado */
export function OptimizationPanel({
  uso,
  onUso,
  analisis,
  cargando,
  desactualizado,
  puedeAnalizar,
  guardaEnCuenta,
  onAnalizar,
  onReemplazar,
}: {
  uso: UsoPrevisto;
  onUso: (u: UsoPrevisto) => void;
  analisis: AnalisisOptimizacion | null;
  cargando: boolean;
  desactualizado: boolean;
  puedeAnalizar: boolean;
  /** true si el análisis se guarda junto a la configuración abierta. */
  guardaEnCuenta: boolean;
  onAnalizar: () => void;
  onReemplazar: (a: Alternativa) => void;
}) {
  const vigente = analisis && !desactualizado ? analisis : null;
  const porCategoria = new Map<Categoria, Alternativa[]>();
  for (const a of vigente?.alternativas ?? []) {
    porCategoria.set(a.categoria, [...(porCategoria.get(a.categoria) ?? []), a]);
  }
  const mejoras = vigente?.recomendaciones.filter((h) => h.puntos > 0) ?? [];
  const avisos = vigente?.recomendaciones.filter((h) => h.puntos === 0) ?? [];

  return (
    <section className="tarjeta optimizacion" aria-labelledby="titulo-optimizacion">
      <div className="tarjeta-cuerpo pila">
        <div className="fila optimizacion-cabecera">
          <div>
            <span className="eyebrow" style={{ color: "var(--c-acento)" }}>
              Optimización
            </span>
            <h2 id="titulo-optimizacion" style={{ fontSize: "var(--t-xl)" }}>
              ¿Está bien aprovechado tu dinero?
            </h2>
            <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
              Revisa el balance entre procesador y gráfica, la memoria y el disco según el uso que le darás.
            </p>
          </div>
          <div className="fila" style={{ flexWrap: "wrap" }}>
            <div className="segmentado" role="radiogroup" aria-label="Uso previsto">
              {USOS.map((u) => (
                <button
                  key={u.id}
                  role="radio"
                  aria-checked={uso === u.id}
                  className={uso === u.id ? "activo" : ""}
                  onClick={() => onUso(u.id)}
                >
                  {u.etiqueta}
                </button>
              ))}
            </div>
            <button className="btn btn-primario" onClick={onAnalizar} disabled={!puedeAnalizar || cargando}>
              {cargando ? <Loader2 size={16} className="girar" /> : <Gauge size={16} />}
              {cargando ? "Analizando…" : vigente ? "Analizar de nuevo" : "Analizar"}
            </button>
          </div>
        </div>

        {!puedeAnalizar && (
          <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
            Elige al menos el procesador para analizar la optimización.
          </p>
        )}
        {analisis && desactualizado && (
          <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
            Cambiaste piezas o el uso: vuelve a analizar.
          </p>
        )}

        {vigente && (
          <>
            <div className="optimizacion-resumen">
              <Puntaje valor={vigente.puntaje} umbral={vigente.umbralAlternativas} />
              <div className="pila" style={{ gap: 6 }}>
                <strong style={{ fontSize: "var(--t-lg)" }}>
                  {vigente.cuelloBotella ?? "Sin cuellos de botella para " + USOS.find((u) => u.id === vigente.uso)?.etiqueta.toLowerCase()}
                </strong>
                <span className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
                  {vigente.balance !== null && <>Balance gráfica/procesador: {vigente.balance.toFixed(2).replace(".", ",")} · </>}
                  {vigente.consumoEstimadoW > 0 && <>Consumo estimado: {vigente.consumoEstimadoW} W · </>}
                  {guardaEnCuenta ? "Guardado con tu configuración" : "Análisis sin guardar"}
                </span>
              </div>
            </div>

            {mejoras.length > 0 && (
              <div className="pila" style={{ gap: "var(--s-2)" }}>
                <h3 className="optimizacion-subtitulo">Qué mejorar</h3>
                {mejoras.map((h, i) => (
                  <div key={i} className="hallazgo">
                    <span className="hallazgo-puntos" aria-label={`resta ${h.puntos} puntos`}>
                      −{h.puntos}
                    </span>
                    <div>
                      <strong>{h.titulo}</strong>
                      <p>{h.explicacion}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {vigente.fortalezas.length > 0 && (
              <div className="pila" style={{ gap: "var(--s-2)" }}>
                <h3 className="optimizacion-subtitulo">Lo que está bien</h3>
                <ul className="fortalezas">
                  {vigente.fortalezas.map((f, i) => (
                    <li key={i}>
                      <CircleCheck size={16} aria-hidden="true" /> {f}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {avisos.map((h, i) => (
              <p key={i} className="texto-2 fila" style={{ fontSize: "var(--t-xs)", alignItems: "flex-start", gap: 6 }}>
                <Info size={14} aria-hidden="true" style={{ flexShrink: 0, marginTop: 2 }} />
                <span>
                  <strong>{h.titulo}.</strong> {h.explicacion}
                </span>
              </p>
            ))}

            {vigente.buscoAlternativas && (
              <div className="pila" style={{ gap: "var(--s-3)" }}>
                <h3 className="optimizacion-subtitulo">Alternativas en stock</h3>
                {porCategoria.size === 0 ? (
                  <p className="texto-2 fila" style={{ fontSize: "var(--t-sm)", gap: 6 }}>
                    <TriangleAlert size={16} aria-hidden="true" /> No hay piezas en stock que mejoren este armado cambiando una
                    sola pieza.
                  </p>
                ) : (
                  [...porCategoria.entries()].map(([cat, lista]) => (
                    <div key={cat} className="pila" style={{ gap: "var(--s-2)" }}>
                      <span className="fila texto-2" style={{ fontSize: "var(--t-sm)", gap: 6 }}>
                        <IconoCategoria categoria={cat} size={16} /> {etiquetaCategoria(cat)}
                        {lista[0].reemplaza ? ` · en lugar de ${lista[0].reemplaza.nombre}` : " · agregar"}
                      </span>
                      <div className="alternativas">
                        {lista.map((a) => (
                          <div key={a.producto.id} className="alternativa">
                            <div className="pila" style={{ gap: 4, minWidth: 0 }}>
                              <strong>{a.producto.nombre}</strong>
                              <span className="texto-2" style={{ fontSize: "var(--t-xs)" }}>
                                {a.motivo}
                              </span>
                              <span className="fila" style={{ gap: 8, fontSize: "var(--t-sm)" }}>
                                <PriceDisplay monto={a.producto.precio} tamano="sm" />
                                <span className={a.diferenciaPrecio > 0 ? "texto-2" : "texto-exito"}>
                                  {a.diferenciaPrecio === 0
                                    ? "mismo precio"
                                    : `${a.diferenciaPrecio > 0 ? "+" : "−"}${bs(Math.abs(a.diferenciaPrecio))}`}
                                </span>
                              </span>
                            </div>
                            <div className="pila" style={{ gap: 6, alignItems: "flex-end" }}>
                              <span className="fila" style={{ gap: 4, fontSize: "var(--t-sm)" }}>
                                {vigente.puntaje} <ArrowRight size={14} aria-hidden="true" /> <strong>{a.puntajeNuevo}</strong>
                              </span>
                              <button className="btn btn-secundario btn-sm" onClick={() => onReemplazar(a)}>
                                <Replace size={14} /> {a.reemplaza ? "Reemplazar" : "Agregar"}
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
            {!vigente.buscoAlternativas && (
              <p className="texto-2" style={{ fontSize: "var(--t-xs)" }}>
                Con {vigente.umbralAlternativas} puntos o más y sin incompatibilidades no se proponen reemplazos.
              </p>
            )}
            <p className="texto-2" style={{ fontSize: "var(--t-xs)" }}>
              Los umbrales del puntaje son una propuesta del equipo, pendiente de validación.
            </p>
          </>
        )}
      </div>
    </section>
  );
}
