"use client";

import { RefreshCw } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { StatusBadge } from "@/components/Badges";
import { ErrorState, LoadingSkeleton } from "@/components/Estados";
import { useAuth } from "@/lib/auth";

interface Estado {
  estado: "UP" | "DEGRADADO";
  fecha: string;
  servicios: { servicio: string; estado: string; latenciaMs: number }[];
}

const NOMBRES: Record<string, string> = {
  usuarios: "Usuarios",
  catalogo: "Catálogo",
  compatibilidad: "Compatibilidad",
  optimizacion: "Optimización",
  inventario: "Inventario",
  "pedidos-pagos": "Pedidos y pagos",
  notificaciones: "Notificaciones",
  auditoria: "Auditoría",
  reportes: "Reportes",
};

/** HU-25 / RF-23 (primera parte): salud de cada microservicio, leída en vivo del gateway. */
export default function EstadoServicios() {
  const { pedirConSesion } = useAuth();
  const [datos, setDatos] = useState<Estado | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);
  const [correo, setCorreo] = useState<{ pendientes: number; enviadas: number; fallidas: number; circuito: string } | null>(null);

  const cargar = useCallback(() => {
    setCargando(true);
    setError(null);
    pedirConSesion<Estado>("/api/estado")
      .then(setDatos)
      .catch((e: Error) => setError(e.message))
      .finally(() => setCargando(false));
    pedirConSesion<typeof correo>("/api/notificaciones/resumen").then(setCorreo).catch(() => setCorreo(null));
  }, [pedirConSesion]);

  useEffect(() => {
    cargar();
    const t = setInterval(cargar, 15000);
    return () => clearInterval(t);
  }, [cargar]);

  const caidos = datos?.servicios.filter((s) => s.estado !== "UP").length ?? 0;

  return (
    <>
      <div className="panel-cabecera">
        <div>
          <span className="eyebrow">Sistema</span>
          <h1>Estado de servicios</h1>
          <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
            {datos ? `Actualizado ${new Date(datos.fecha).toLocaleTimeString("es-BO")} · se refresca cada 15 s` : "Consultando…"}
          </p>
        </div>
        <button className="btn btn-secundario" onClick={cargar} disabled={cargando}>
          <RefreshCw size={16} className={cargando ? "girar" : undefined} /> Actualizar
        </button>
      </div>

      {error ? (
        <ErrorState mensaje={error} onReintentar={cargar} />
      ) : !datos ? (
        <LoadingSkeleton alto={200} radio={16} />
      ) : (
        <div className="pila">
          <div className={`alerta ${caidos ? "alerta-aviso" : "alerta-exito"}`} role="status">
            {caidos
              ? `${caidos} servicio${caidos === 1 ? "" : "s"} sin responder. El resto sigue operando (modo degradado).`
              : "Todos los servicios responden."}
          </div>
          <div className="servicios">
            {datos.servicios.map((s) => (
              <div className="tarjeta servicio" key={s.servicio}>
                <div>
                  <strong>{NOMBRES[s.servicio] ?? s.servicio}</strong>
                  <div className="texto-2" style={{ fontSize: "var(--t-xs)" }}>{s.latenciaMs} ms</div>
                </div>
                <StatusBadge tono={s.estado === "UP" ? "exito" : "error"}>{s.estado === "UP" ? "En línea" : "Caído"}</StatusBadge>
              </div>
            ))}
          </div>
          <div className="tarjeta servicio">
            <div>
              <strong>Envío de correos</strong>
              <div className="texto-2" style={{ fontSize: "var(--t-xs)" }}>
                {correo
                  ? `${correo.pendientes} en cola · ${correo.enviadas} enviados · ${correo.fallidas} sin destinatario`
                  : "Sin datos del servicio de notificaciones"}
              </div>
            </div>
            {correo && (
              <StatusBadge tono={correo.circuito === "CLOSED" ? "exito" : correo.circuito === "OPEN" ? "error" : "aviso"}>
                {correo.circuito === "CLOSED" ? "Circuito cerrado" : correo.circuito === "OPEN" ? "Circuito abierto" : "Probando"}
              </StatusBadge>
            )}
          </div>
          <p className="texto-2" style={{ fontSize: "var(--t-xs)" }}>
            Cada servicio protege sus llamadas a los demás con un Circuit Breaker: si uno cae, solo su función responde
            «no disponible» y el resto sigue. Métricas, latencias y alertas en Grafana (perfil «monitoreo» de Docker Compose).
          </p>
        </div>
      )}
    </>
  );
}
