import { EmptyState } from "./Estados";

export interface Columna<T> {
  clave: string;
  titulo: string;
  render: (fila: T) => React.ReactNode;
  numerica?: boolean;
}

/** Tabla para los paneles (admin, inventario, POS) */
export function DataTable<T>({
  columnas,
  filas,
  claveFila,
  vacio = "No hay registros.",
  titulo,
}: {
  columnas: Columna<T>[];
  filas: T[];
  claveFila: (fila: T) => string | number;
  vacio?: string;
  titulo?: string;
}) {
  if (filas.length === 0) return <EmptyState titulo={vacio} />;
  return (
    <div className="tabla-envoltura" role="region" aria-label={titulo} tabIndex={0}>
      <table className="tabla">
        {titulo && <caption className="sr-only">{titulo}</caption>}
        <thead>
          <tr>
            {columnas.map((c) => (
              <th key={c.clave} scope="col" className={c.numerica ? "num" : undefined}>
                {c.titulo}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.map((f) => (
            <tr key={claveFila(f)}>
              {columnas.map((c) => (
                <td key={c.clave} className={c.numerica ? "num" : undefined}>
                  {c.render(f)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
