import { CircleX, PackageSearch } from "lucide-react";

/** Marcador de carga con la forma aproximada del contenido que viene. */
export function LoadingSkeleton({ alto = 16, ancho = "100%", radio }: { alto?: number; ancho?: number | string; radio?: number }) {
  return <div className="esqueleto" style={{ height: alto, width: ancho, borderRadius: radio }} aria-hidden="true" />;
}

/** Esqueleto de una tarjeta de producto, para la rejilla del catálogo. */
export function ProductCardSkeleton() {
  return (
    <div className="prod-card" aria-hidden="true">
      <div className="esqueleto" style={{ aspectRatio: "4 / 3", borderRadius: 0 }} />
      <div className="prod-cuerpo">
        <LoadingSkeleton alto={10} ancho="40%" />
        <LoadingSkeleton alto={18} ancho="85%" />
        <LoadingSkeleton alto={14} ancho="60%" />
        <div className="prod-pie">
          <LoadingSkeleton alto={22} ancho={90} />
          <LoadingSkeleton alto={34} ancho={90} />
        </div>
      </div>
    </div>
  );
}

export function EmptyState({
  titulo,
  mensaje,
  accion,
  icono,
}: {
  titulo: string;
  mensaje?: React.ReactNode;
  accion?: React.ReactNode;
  icono?: React.ReactNode;
}) {
  return (
    <div className="estado-vacio">
      <div className="icono">{icono ?? <PackageSearch size={26} />}</div>
      <h3>{titulo}</h3>
      {mensaje && <p>{mensaje}</p>}
      {accion}
    </div>
  );
}

/** Error al cargar datos, con opción de reintentar. */
export function ErrorState({ mensaje, onReintentar }: { mensaje: string; onReintentar?: () => void }) {
  return (
    <div className="estado-vacio error" role="alert">
      <div className="icono">
        <CircleX size={26} />
      </div>
      <h3>No pudimos cargar la información</h3>
      <p>{mensaje}</p>
      {onReintentar && (
        <button className="btn btn-secundario btn-sm" onClick={onReintentar}>
          Reintentar
        </button>
      )}
    </div>
  );
}
