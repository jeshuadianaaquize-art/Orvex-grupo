"use client";

import { useCallback, useEffect, useState } from "react";
import { DataTable } from "@/components/DataTable";
import { ErrorState, LoadingSkeleton } from "@/components/Estados";
import { Pagination } from "@/components/Pagination";
import { fechaUtc } from "@/lib/api";
import { useAuth } from "@/lib/auth";

interface Registro {
  id: number;
  usuarioId: number | null;
  accion: string;
  entidad: string;
  detalle: string | null;
  fecha: string;
}

interface Pagina {
  elementos: Registro[];
  pagina: number;
  totalPaginas: number;
  total: number;
}

/** Nombre legible de cada acción (las claves son los tipos de evento de la cola). */
const ACCIONES: Record<string, string> = {
  "producto.precio_cambiado": "Cambio de precio",
  "producto.creado": "Producto creado",
  "producto.estado_cambiado": "Producto publicado / retirado",
  "inventario.ajuste": "Ajuste de stock",
  "inventario.ingreso": "Ingreso de mercadería",
  "inventario.devolucion": "Devolución al stock",
  "inventario.minimo_cambiado": "Cambio de stock mínimo",
  "stock.bajo": "Alerta de stock bajo",
  "venta.anulada": "Venta anulada",
  "usuario.rol_cambiado": "Cambio de rol",
  "usuario.estado_cambiado": "Usuario bloqueado / activado",
  "regla.creada": "Regla creada",
  "regla.modificada": "Regla modificada",
  "regla.eliminada": "Regla eliminada",
  "pedido.creado": "Pedido creado",
  "pago.confirmado": "Pago confirmado",
  "pago.conciliacion": "Pago a conciliar (reembolso)",
  "pedido.estado_cambiado": "Pedido preparado / entregado",
  "pedido.cancelado": "Pedido cancelado",
};

function Detalle({ texto }: { texto: string | null }) {
  if (!texto) return <>—</>;
  try {
    const d = JSON.parse(texto) as Record<string, unknown>;
    return (
      <span className="detalle-auditoria">
        {Object.entries(d)
          .filter(([k, v]) => k !== "servicio" && v !== "" && v != null)
          .map(([k, v]) => `${k}: ${typeof v === "object" ? JSON.stringify(v) : String(v)}`)
          .join(" · ")}
      </span>
    );
  } catch {
    return <>{texto}</>;
  }
}

/** Auditoría (RF-26, HU-21): acciones críticas con usuario y fecha. Solo lectura. */
export default function AuditoriaAdmin() {
  const { pedirConSesion } = useAuth();
  const [acciones, setAcciones] = useState<string[]>([]);
  const [accion, setAccion] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [pagina, setPagina] = useState(0);
  const [datos, setDatos] = useState<Pagina | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    pedirConSesion<string[]>("/api/auditoria/acciones").then(setAcciones).catch(() => setAcciones([]));
  }, [pedirConSesion]);

  const cargar = useCallback(() => {
    setError(null);
    const q = new URLSearchParams({ pagina: String(pagina) });
    if (accion) q.set("accion", accion);
    if (desde) q.set("desde", desde);
    if (hasta) q.set("hasta", hasta);
    pedirConSesion<Pagina>(`/api/auditoria?${q}`).then(setDatos).catch((e: Error) => setError(e.message));
  }, [pedirConSesion, accion, desde, hasta, pagina]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  return (
    <>
      <div className="panel-cabecera">
        <div>
          <span className="eyebrow">Sistema</span>
          <h1>Auditoría</h1>
          <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
            Cambios de precio, ajustes de stock, ventas anuladas, cambios de rol y más, con quién y cuándo. No se puede editar ni borrar.
          </p>
        </div>
      </div>
      <div className="tarjeta tarjeta-cuerpo barra-io" style={{ marginBottom: "var(--s-4)" }}>
        <label>
          Acción
          <select className="entrada" value={accion} onChange={(e) => { setPagina(0); setAccion(e.target.value); }}>
            <option value="">Todas</option>
            {acciones.map((a) => <option key={a} value={a}>{ACCIONES[a] ?? a}</option>)}
          </select>
        </label>
        <label>Desde<input className="entrada" type="date" value={desde} onChange={(e) => { setPagina(0); setDesde(e.target.value); }} /></label>
        <label>Hasta<input className="entrada" type="date" value={hasta} onChange={(e) => { setPagina(0); setHasta(e.target.value); }} /></label>
      </div>
      {error ? (
        <ErrorState mensaje={error} onReintentar={cargar} />
      ) : !datos ? (
        <LoadingSkeleton alto={240} radio={16} />
      ) : (
        <div className="pila">
          <DataTable
            titulo="Registro de auditoría"
            filas={datos.elementos}
            claveFila={(r) => r.id}
            vacio="No hay acciones registradas con esos filtros."
            columnas={[
              { clave: "f", titulo: "Fecha", render: (r) => fechaUtc(r.fecha).toLocaleString("es-BO", { dateStyle: "short", timeStyle: "medium" }) },
              { clave: "a", titulo: "Acción", render: (r) => <strong>{ACCIONES[r.accion] ?? r.accion}</strong> },
              { clave: "u", titulo: "Usuario", render: (r) => (r.usuarioId == null ? "Sistema" : `#${r.usuarioId}`) },
              { clave: "e", titulo: "Entidad", render: (r) => r.entidad },
              { clave: "d", titulo: "Detalle", render: (r) => <Detalle texto={r.detalle} /> },
            ]}
          />
          <Pagination pagina={datos.pagina} totalPaginas={datos.totalPaginas} onCambiar={setPagina} />
          <p className="texto-2" style={{ fontSize: "var(--t-xs)" }}>{datos.total} registros.</p>
        </div>
      )}
    </>
  );
}
