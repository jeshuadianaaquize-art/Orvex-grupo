import { ChevronLeft, ChevronRight } from "lucide-react";

/** Páginas visibles: primera, última, la actual y sus vecinas; el resto se resume con "…". */
export function paginasVisibles(actual: number, total: number): (number | "…")[] {
  const set = new Set([1, total, actual - 1, actual, actual + 1].filter((p) => p >= 1 && p <= total));
  const orden = [...set].sort((a, b) => a - b);
  const r: (number | "…")[] = [];
  orden.forEach((p, i) => {
    if (i > 0 && p - orden[i - 1] > 1) r.push("…");
    r.push(p);
  });
  return r;
}

export function Pagination({
  pagina,
  totalPaginas,
  onCambiar,
}: {
  pagina: number;
  totalPaginas: number;
  onCambiar: (p: number) => void;
}) {
  if (totalPaginas <= 1) return null;
  return (
    <nav className="paginacion" aria-label="Paginación">
      <button
        className="btn btn-secundario btn-sm btn-icono"
        disabled={pagina === 1}
        onClick={() => onCambiar(pagina - 1)}
        aria-label="Página anterior"
      >
        <ChevronLeft size={16} />
      </button>
      {paginasVisibles(pagina, totalPaginas).map((p, i) =>
        p === "…" ? (
          <span key={`e${i}`} className="texto-2" aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={p}
            className="btn btn-secundario btn-sm btn-icono"
            aria-current={p === pagina ? "page" : undefined}
            onClick={() => onCambiar(p)}
          >
            {p}
          </button>
        ),
      )}
      <button
        className="btn btn-secundario btn-sm btn-icono"
        disabled={pagina === totalPaginas}
        onClick={() => onCambiar(pagina + 1)}
        aria-label="Página siguiente"
      >
        <ChevronRight size={16} />
      </button>
    </nav>
  );
}
