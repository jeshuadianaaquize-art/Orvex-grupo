"use client";

import { Info, SlidersHorizontal } from "lucide-react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { EmptyState, ErrorState, ProductCardSkeleton } from "@/components/Estados";
import { FILTROS_VACIOS, FilterPanel, type Filtros } from "@/components/FilterPanel";
import { Pagination } from "@/components/Pagination";
import { ProductCard } from "@/components/ProductCard";
import { useToast } from "@/components/Toast";
import { buscarProductos, esCategoria, etiquetaCategoria, type Busqueda, type Orden, type Producto } from "@/lib/api";
import { useCart } from "@/lib/cart";

const POR_PAGINA = 12;
const ORDENES: Orden[] = ["PRECIO_ASC", "PRECIO_DESC", "NOMBRE"];

export default function TiendaPagina() {
  return (
    <Suspense fallback={null}>
      <Tienda />
    </Suspense>
  );
}

/** Los filtros viven en la URL */
function leerUrl(p: URLSearchParams): { filtros: Filtros; orden: Orden; pagina: number } {
  const cat = p.get("cat");
  const max = Number(p.get("max"));
  const orden = p.get("orden") as Orden;
  return {
    filtros: {
      q: p.get("q") ?? "",
      categoria: esCategoria(cat) ? cat : "",
      marcas: p.getAll("marca"),
      precioMax: Number.isFinite(max) && max > 0 ? max : null,
      socket: p.get("socket") ?? "",
      tipoRam: p.get("ram") ?? "",
      soloStock: p.get("stock") === "1",
    },
    orden: ORDENES.includes(orden) ? orden : "PRECIO_ASC",
    pagina: Math.max(0, Number(p.get("pagina") ?? 1) - 1) || 0,
  };
}

function escribirUrl(f: Filtros, orden: Orden, pagina: number): string {
  const u = new URLSearchParams();
  if (f.q.trim()) u.set("q", f.q.trim());
  if (f.categoria) u.set("cat", f.categoria);
  f.marcas.forEach((m) => u.append("marca", m));
  if (f.precioMax != null) u.set("max", String(f.precioMax));
  if (f.socket) u.set("socket", f.socket);
  if (f.tipoRam) u.set("ram", f.tipoRam);
  if (f.soloStock) u.set("stock", "1");
  if (orden !== "PRECIO_ASC") u.set("orden", orden);
  if (pagina > 0) u.set("pagina", String(pagina + 1));
  return u.toString();
}

function Tienda() {
  const params = useSearchParams();
  const router = useRouter();
  const ruta = usePathname();
  const inicial = useMemo(() => leerUrl(params), [params]);
  const [texto, setTexto] = useState(inicial.filtros.q);
  const [datos, setDatos] = useState<Busqueda | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(true);
  const [verFiltros, setVerFiltros] = useState(false);
  const [agregados, setAgregados] = useState<number[]>([]);
  const peticion = useRef(0);
  const { agregarVarios } = useCart();
  const toast = useToast();
  const { filtros, orden, pagina } = inicial;

  // El texto se escribe localmente y se aplica a la URL al dejar de escribir.
  useEffect(() => setTexto(inicial.filtros.q), [inicial.filtros.q]);
  useEffect(() => {
    if (texto === filtros.q) return;
    const t = setTimeout(() => navegar({ ...filtros, q: texto }, orden, 0), 350);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [texto]);

  const navegar = useCallback(
    (f: Filtros, o: Orden, p: number) => {
      const qs = escribirUrl(f, o, p);
      router.replace(qs ? `${ruta}?${qs}` : ruta, { scroll: false });
    },
    [router, ruta],
  );

  const cargar = useCallback(() => {
    const n = ++peticion.current;
    setCargando(true);
    setError(null);
    buscarProductos({
      q: filtros.q,
      categoria: filtros.categoria,
      marcas: filtros.marcas,
      precioMax: filtros.precioMax,
      socket: filtros.socket,
      tipoRam: filtros.tipoRam,
      disponible: filtros.soloStock,
      orden,
      pagina,
      tamano: POR_PAGINA,
    })
      .then((d) => n === peticion.current && setDatos(d))
      .catch((e: Error) => n === peticion.current && setError(e.message))
      .finally(() => n === peticion.current && setCargando(false));
    // Se vuelve a pedir cada vez que cambia la URL (filtros, orden o página).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params]);
  useEffect(cargar, [cargar]);

  function agregar(p: Producto) {
    void agregarVarios([p]).then((ok) => {
      if (!ok) return;
      setAgregados((a) => [...a, p.id]);
      toast(`${p.nombre} se agregó al carrito`);
    });
  }

  const resultados = datos?.resultados;
  return (
    <div className="contenedor catalogo">
      <div className={verFiltros ? "" : "oculto-movil"}>
        <FilterPanel
          filtros={{ ...filtros, q: texto }}
          facetas={datos?.facetas ?? null}
          onCambiar={(f) => {
            if (f.q !== texto) setTexto(f.q);
            else navegar(f, orden, 0);
          }}
        />
      </div>

      <section aria-labelledby="titulo-catalogo" aria-busy={cargando}>
        <div className="barra-catalogo">
          <div>
            <span className="eyebrow">Catálogo</span>
            <h1 id="titulo-catalogo">{filtros.categoria ? etiquetaCategoria(filtros.categoria) : "Todos los componentes"}</h1>
            <p className="texto-2" style={{ fontSize: "var(--t-sm)" }} aria-live="polite">
              {resultados ? `${resultados.total} producto${resultados.total === 1 ? "" : "s"}` : "Cargando…"}
              {filtros.q && ` para “${filtros.q}”`}
            </p>
          </div>
          <div className="fila">
            <button className="btn btn-secundario btn-sm solo-movil" onClick={() => setVerFiltros((v) => !v)} aria-expanded={verFiltros}>
              <SlidersHorizontal size={16} /> Filtros
            </button>
            <select
              className="entrada"
              style={{ height: 34, width: "auto" }}
              value={orden}
              onChange={(e) => navegar(filtros, e.target.value as Orden, 0)}
              aria-label="Ordenar"
            >
              <option value="PRECIO_ASC">Precio: menor a mayor</option>
              <option value="PRECIO_DESC">Precio: mayor a menor</option>
              <option value="NOMBRE">Nombre (A–Z)</option>
            </select>
          </div>
        </div>

        {datos && !datos.disponibilidadConfirmada && (
          <div className="alerta alerta-aviso" role="status" style={{ marginBottom: "var(--s-4)" }}>
            <Info aria-hidden="true" />
            No pudimos consultar el inventario en este momento: la disponibilidad se confirmará al comprar.
          </div>
        )}

        {error ? (
          <ErrorState mensaje={`No se pudo cargar el catálogo. ${error}`} onReintentar={cargar} />
        ) : !resultados ? (
          <div className="rejilla">
            {Array.from({ length: 6 }, (_, i) => (
              <ProductCardSkeleton key={i} />
            ))}
          </div>
        ) : resultados.total === 0 ? (
          <EmptyState
            titulo="Ningún producto coincide"
            mensaje="Prueba con otra búsqueda o quita algunos filtros."
            accion={
              <button className="btn btn-secundario btn-sm" onClick={() => navegar(FILTROS_VACIOS, orden, 0)}>
                Limpiar filtros
              </button>
            }
          />
        ) : (
          <>
            <div className="rejilla entrar" key={params.toString()} style={{ opacity: cargando ? 0.6 : 1, transition: "opacity var(--dur-2)" }}>
              {resultados.elementos.map((p) => (
                <ProductCard key={p.id} producto={p} onAgregar={agregar} agregado={agregados.includes(p.id)} />
              ))}
            </div>
            <Pagination
              pagina={resultados.pagina + 1}
              totalPaginas={resultados.totalPaginas}
              onCambiar={(p) => {
                navegar(filtros, orden, p - 1);
                document.getElementById("titulo-catalogo")?.scrollIntoView({ behavior: "smooth", block: "start" });
              }}
            />
          </>
        )}
      </section>
    </div>
  );
}
