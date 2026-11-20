import type { Metadata } from "next";
import { CREDITOS_FOTOS } from "@/lib/creditos-fotos";

export const metadata: Metadata = { title: "Créditos de las fotos" };

/** Autor, licencia y fuente de cada foto de referencia, como piden sus licencias libres. */
export default function Creditos() {
  return (
    <section className="seccion contenedor">
      <div className="seccion-cabecera">
        <div>
          <span className="eyebrow">Fotos de referencia</span>
          <h2>Créditos de las fotos</h2>
          <p className="texto-2" style={{ marginTop: 6, maxWidth: "46rem" }}>
            Las fotos de los productos de demostración vienen de Wikimedia Commons y tienen licencia libre. Algunas
            muestran el modelo exacto y otras un componente del mismo tipo. Se redujeron de tamaño para la web; no se
            modificó su contenido.
          </p>
        </div>
      </div>
      <div className="creditos">
        {CREDITOS_FOTOS.map((c) => (
          <article className="tarjeta credito" key={c.pagina}>
            {/* eslint-disable-next-line @next/next/no-img-element -- fotos estáticas de la propia web */}
            <img src={`/img/productos/${c.skus[0].toLowerCase()}.jpg`} alt={c.titulo} loading="lazy" />
            <div className="pila" style={{ gap: 4 }}>
              <a href={c.pagina} target="_blank" rel="noopener noreferrer" className="credito-titulo">
                {c.titulo}
              </a>
              <span className="texto-2">Autor: {c.autor}</span>
              <span className="texto-2">
                Licencia:{" "}
                {c.licenciaUrl ? (
                  <a href={c.licenciaUrl} target="_blank" rel="noopener noreferrer license">
                    {c.licencia}
                  </a>
                ) : (
                  c.licencia
                )}
              </span>
              <span className="specs" style={{ marginTop: 4 }}>
                {c.skus.map((s) => (
                  <span className="spec" key={s}>
                    {s}
                  </span>
                ))}
              </span>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
