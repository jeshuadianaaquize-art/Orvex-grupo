"use client";

import { ArrowRight, ChevronRight, Cpu, ShoppingCart } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StockBadge } from "@/components/Badges";
import { EmptyState, ErrorState, LoadingSkeleton } from "@/components/Estados";
import { ImagenProducto } from "@/components/ImagenProducto";
import { PriceDisplay } from "@/components/PriceDisplay";
import { ProductCard } from "@/components/ProductCard";
import { useToast } from "@/components/Toast";
import { detalleProducto, ErrorApi, etiquetaCategoria, type Producto } from "@/lib/api";
import { useCart } from "@/lib/cart";
import { specsLegibles } from "@/lib/specs";

/** Margen de seguridad de la fuente que usa la regla de consumo (valor propuesto, ver docs). */
const MARGEN_FUENTE = 1.3;

interface Pista {
  texto: string;
  href?: string;
}

const lista = (v?: string) => (v ? v.split(",").map((x) => x.trim()).filter(Boolean) : []);

/** Qué conviene revisar al combinar este producto con otros, a partir de sus especificaciones. */
function pistas(p: Producto): Pista[] {
  const s = p.specs;
  const r: Pista[] = [];
  switch (p.categoria) {
    case "PROCESADOR":
      if (s.socket) {
        r.push({ texto: `Placas madre con socket ${s.socket}`, href: `/tienda?cat=PLACA_MADRE&socket=${encodeURIComponent(s.socket)}` });
        r.push({ texto: `Coolers compatibles con ${s.socket}`, href: `/tienda?cat=REFRIGERACION&socket=${encodeURIComponent(s.socket)}` });
      }
      if (s.tdp_w) r.push({ texto: `Su consumo (${s.tdp_w} W) se suma al calcular la fuente de poder.` });
      break;
    case "PLACA_MADRE":
      if (s.socket) r.push({ texto: `Procesadores con socket ${s.socket}`, href: `/tienda?cat=PROCESADOR&socket=${encodeURIComponent(s.socket)}` });
      if (s.tipo_ram) r.push({ texto: `Memorias ${s.tipo_ram}`, href: `/tienda?cat=RAM&ram=${encodeURIComponent(s.tipo_ram)}` });
      if (s.factor_forma) r.push({ texto: `Necesita un gabinete que admita placas ${s.factor_forma}.`, href: "/tienda?cat=GABINETE" });
      break;
    case "RAM":
      if (s.tipo_ram) r.push({ texto: `Placas madre que usan ${s.tipo_ram}`, href: `/tienda?cat=PLACA_MADRE&ram=${encodeURIComponent(s.tipo_ram)}` });
      break;
    case "TARJETA_GRAFICA":
      if (s.largo_mm) r.push({ texto: `Necesita un gabinete con espacio para gráficas de al menos ${s.largo_mm} mm.`, href: "/tienda?cat=GABINETE" });
      if (s.tdp_w) r.push({ texto: `Su consumo (${s.tdp_w} W) se suma al calcular la fuente de poder.`, href: "/tienda?cat=FUENTE" });
      break;
    case "GABINETE":
      if (s.factores_forma) r.push({ texto: `Admite placas ${lista(s.factores_forma).join(", ")}.` });
      if (s.max_largo_gpu_mm) r.push({ texto: `Tarjetas gráficas de hasta ${s.max_largo_gpu_mm} mm de largo.` });
      if (s.max_altura_cooler_mm) r.push({ texto: `Coolers de hasta ${s.max_altura_cooler_mm} mm de altura.` });
      break;
    case "REFRIGERACION":
      lista(s.sockets).forEach((x) =>
        r.push({ texto: `Procesadores con socket ${x}`, href: `/tienda?cat=PROCESADOR&socket=${encodeURIComponent(x)}` }),
      );
      if (s.altura_mm) r.push({ texto: `Mide ${s.altura_mm} mm: el gabinete debe admitir esa altura.` });
      break;
    case "FUENTE":
      if (s.potencia_w)
        r.push({
          texto: `Con el margen de seguridad de ${Math.round((MARGEN_FUENTE - 1) * 100)} % que aplica Orvex, cubre equipos de hasta unos ${Math.floor(Number(s.potencia_w) / MARGEN_FUENTE)} W estimados.`,
        });
      break;
    case "ALMACENAMIENTO":
      if (s.interfaz) r.push({ texto: `Interfaz ${s.interfaz}: la placa madre necesita un puerto de ese tipo.` });
      break;
  }
  return r;
}

export default function FichaProducto() {
  const { id } = useParams<{ id: string }>();
  const [datos, setDatos] = useState<{ producto: Producto; relacionados: Producto[] } | null>(null);
  const [error, setError] = useState<ErrorApi | Error | null>(null);
  const { agregarVarios } = useCart();
  const toast = useToast();

  const cargar = useCallback(() => {
    setError(null);
    setDatos(null);
    const n = Number(id);
    if (!Number.isInteger(n) || n <= 0) {
      setError(new ErrorApi("El producto no existe.", 404));
      return;
    }
    detalleProducto(n).then(setDatos).catch(setError);
  }, [id]);
  useEffect(cargar, [cargar]);

  useEffect(() => {
    if (datos) document.title = `${datos.producto.nombre} · Orvex`;
  }, [datos]);

  if (error) {
    return (
      <div className="contenedor" style={{ paddingBlock: "var(--s-12)" }}>
        {error instanceof ErrorApi && error.estado === 404 ? (
          <EmptyState
            titulo="Este producto no está disponible"
            mensaje="Puede que ya no esté a la venta. Revisa el catálogo para ver opciones similares."
            accion={<Link href="/tienda" className="btn btn-primario btn-sm">Ver el catálogo</Link>}
          />
        ) : (
          <ErrorState mensaje={error.message} onReintentar={cargar} />
        )}
      </div>
    );
  }

  if (!datos) {
    return (
      <div className="contenedor ficha" aria-busy="true">
        <LoadingSkeleton alto={420} radio={22} />
        <div className="pila">
          <LoadingSkeleton alto={14} ancho="30%" />
          <LoadingSkeleton alto={40} ancho="80%" />
          <LoadingSkeleton alto={30} ancho="40%" />
          <LoadingSkeleton alto={160} />
        </div>
      </div>
    );
  }

  const p = datos.producto;
  const specs = specsLegibles(p.specs);
  const ayudas = pistas(p);
  return (
    <div className="contenedor">
      <nav className="migas" aria-label="Ruta">
        <Link href="/tienda">Componentes</Link>
        <ChevronRight size={14} aria-hidden="true" />
        <Link href={`/tienda?cat=${p.categoria}`}>{etiquetaCategoria(p.categoria)}</Link>
        <ChevronRight size={14} aria-hidden="true" />
        <span aria-current="page">{p.nombre}</span>
      </nav>

      <div className="ficha">
        <div className="ficha-imagen">
          <ImagenProducto producto={p} tamano="grande" />
        </div>

        <div className="pila" style={{ gap: "var(--s-5)" }}>
          <div>
            <span className="prod-marca">
              {p.marca ?? etiquetaCategoria(p.categoria)} · SKU {p.sku}
            </span>
            <h1>{p.nombre}</h1>
            <div className="fila" style={{ flexWrap: "wrap" }}>
              <PriceDisplay monto={p.precio} tamano="lg" />
              <StockBadge stock={p.stock} enStock={p.enStock} />
              {p.stock != null && p.stock > 0 && (
                <span className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
                  {p.stock} disponible{p.stock === 1 ? "" : "s"}
                </span>
              )}
            </div>
          </div>

          <div className="fila" style={{ flexWrap: "wrap" }}>
            <button
              className="btn btn-primario btn-lg"
              disabled={p.enStock === false}
              onClick={() => {
                void agregarVarios([p]).then((ok) => ok && toast(`${p.nombre} se agregó al carrito`));
              }}
            >
              <ShoppingCart size={18} /> Agregar al carrito
            </button>
            <Link href={`/armador?producto=${p.id}`} className="btn btn-secundario btn-lg">
              <Cpu size={18} /> Armar una PC con este producto
            </Link>
          </div>

          <section className="tarjeta tarjeta-cuerpo" aria-labelledby="t-specs">
            <h2 id="t-specs" style={{ fontSize: "var(--t-lg)", marginBottom: "var(--s-2)" }}>
              Especificaciones técnicas
            </h2>
            {specs.length === 0 ? (
              <p className="texto-2">Este producto todavía no tiene especificaciones cargadas.</p>
            ) : (
              <table className="tabla-specs">
                <tbody>
                  {specs.map((s) => (
                    <tr key={s.clave}>
                      <th scope="row">{s.nombre}</th>
                      <td>{s.valor}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>

          {ayudas.length > 0 && (
            <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-compat">
              <h2 id="t-compat" style={{ fontSize: "var(--t-lg)" }}>Compatibilidad</h2>
              <ul className="compat-lista">
                {ayudas.map((a) => (
                  <li key={a.texto}>
                    <ArrowRight size={16} aria-hidden="true" />
                    {a.href ? (
                      <Link href={a.href} className="enlace" style={{ fontWeight: 500 }}>
                        {a.texto}
                      </Link>
                    ) : (
                      <span>{a.texto}</span>
                    )}
                  </li>
                ))}
              </ul>
              <p className="texto-2" style={{ fontSize: "var(--t-xs)" }}>
                Para comprobar todas las reglas juntas, usa el configurador.
              </p>
            </section>
          )}
        </div>
      </div>

      {datos.relacionados.length > 0 && (
        <section className="seccion" style={{ paddingTop: "var(--s-6)" }} aria-labelledby="t-rel">
          <div className="seccion-cabecera">
            <h2 id="t-rel">Otros en {etiquetaCategoria(p.categoria).toLowerCase()}</h2>
          </div>
          <div className="rejilla entrar">
            {datos.relacionados.map((r) => (
              <ProductCard
                key={r.id}
                producto={r}
                onAgregar={(x) => {
                  void agregarVarios([x]).then((ok) => ok && toast(`${x.nombre} se agregó al carrito`));
                }}
              />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
