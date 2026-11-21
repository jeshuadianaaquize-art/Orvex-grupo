import type { Categoria, Producto } from "@/lib/api";
import { urlFoto } from "@/lib/api";

/** Ilustración neón original de cada categoría (public/img/componentes). */
export function imagenCategoria(categoria: Categoria): string {
  return `/img/componentes/${categoria.toLowerCase()}.svg`;
}

/** Foto del producto si el administrador la subió */
export function ImagenProducto({
  producto,
  tamano = "tarjeta",
}: {
  producto: Pick<Producto, "nombre" | "categoria" | "fotoUrl">;
  tamano?: "tarjeta" | "grande" | "mini";
}) {
  // Fotos de referencia de Wikimedia Commons: las sirve la propia web, no el gateway.
  const referencia = producto.fotoUrl?.startsWith("/img/productos/") ?? false;
  const url = referencia ? producto.fotoUrl : urlFoto(producto.fotoUrl);
  if (url) {
    return (
      <>
        {/* eslint-disable-next-line @next/next/no-img-element -- las fotos vienen del gateway, no de un dominio fijo */}
        <img
          className={`foto-producto foto-${tamano}${referencia ? " foto-referencia" : ""}`}
          src={url}
          alt={producto.nombre}
          loading="lazy"
        />
        {referencia && tamano !== "mini" && <span className="nota nota-foto">Foto de referencia</span>}
      </>
    );
  }
  if (tamano === "mini") {
    // eslint-disable-next-line @next/next/no-img-element -- SVG estático
    return <img className="ilustracion-neon ilus-mini" src={imagenCategoria(producto.categoria)} alt="" loading="lazy" />;
  }
  return (
    <>
      {/* eslint-disable-next-line @next/next/no-img-element -- SVG estático y animado; next/image no lo optimiza */}
      <img className={`ilustracion-neon ilus-${tamano}`} src={imagenCategoria(producto.categoria)} alt="" loading="lazy" />
      <span className="nota">Imagen ilustrativa</span>
    </>
  );
}
