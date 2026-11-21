"use client";

import { Pencil, Plus, Search, TriangleAlert } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { StatusBadge } from "@/components/Badges";
import { DataTable } from "@/components/DataTable";
import { ErrorState, LoadingSkeleton } from "@/components/Estados";
import { ImagenProducto } from "@/components/ImagenProducto";
import { ConfirmDialog } from "@/components/Modal";
import { Pagination } from "@/components/Pagination";
import { PriceDisplay } from "@/components/PriceDisplay";
import { useToast } from "@/components/Toast";
import { ESTADO, SIGUIENTES, type EstadoProducto, type ProductoAdmin } from "@/lib/admin";
import { CATEGORIAS, etiquetaCategoria, type Producto } from "@/lib/api";
import { useAuth } from "@/lib/auth";

const POR_PAGINA = 15;

/** HU-05: lista de productos con su estado, y acciones de publicación. */
export default function AdminProductos() {
  const { pedirConSesion } = useAuth();
  const toast = useToast();
  const [lista, setLista] = useState<ProductoAdmin[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [buscar, setBuscar] = useState("");
  const [categoria, setCategoria] = useState("");
  const [estado, setEstado] = useState("");
  const [pagina, setPagina] = useState(1);
  const [cambio, setCambio] = useState<{ p: ProductoAdmin; estado: EstadoProducto; accion: string } | null>(null);

  const cargar = useCallback(() => {
    setError(null);
    pedirConSesion<ProductoAdmin[]>("/api/catalogo/admin/productos").then(setLista).catch((e: Error) => setError(e.message));
  }, [pedirConSesion]);
  useEffect(cargar, [cargar]);

  const filtrados = useMemo(() => {
    const q = buscar.trim().toLowerCase();
    return (lista ?? []).filter(
      (p) =>
        (!categoria || p.categoria === categoria) &&
        (!estado || p.estado === estado) &&
        (!q || `${p.nombre} ${p.sku} ${p.marca ?? ""}`.toLowerCase().includes(q)),
    );
  }, [lista, buscar, categoria, estado]);
  useEffect(() => setPagina(1), [buscar, categoria, estado]);

  const conteo = useMemo(() => {
    const c: Record<string, number> = {};
    (lista ?? []).forEach((p) => (c[p.estado] = (c[p.estado] ?? 0) + 1));
    return c;
  }, [lista]);

  async function aplicar(c: { p: ProductoAdmin; estado: EstadoProducto }) {
    setCambio(null);
    try {
      await pedirConSesion(`/api/catalogo/admin/productos/${c.p.id}/estado`, { metodo: "PATCH", cuerpo: { estado: c.estado } });
      toast(`${c.p.nombre}: ${ESTADO[c.estado].texto.toLowerCase()}`);
      cargar();
    } catch (e) {
      toast(e instanceof Error ? e.message : "No se pudo cambiar el estado", "error");
    }
  }

  return (
    <>
      <div className="panel-cabecera">
        <div>
          <span className="eyebrow">Catálogo</span>
          <h1>Productos</h1>
          <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
            {lista
              ? `${lista.length} productos · ${conteo.PUBLICADO ?? 0} publicados · ${conteo.BORRADOR ?? 0} en borrador · ${conteo.INACTIVO ?? 0} inactivos`
              : "Cargando…"}
          </p>
        </div>
        <Link href="/admin/productos/nuevo" className="btn btn-primario">
          <Plus size={16} /> Nuevo producto
        </Link>
      </div>

      <div className="barra-filtros">
        <div className="busqueda buscador" style={{ display: "block", maxWidth: "none", margin: 0 }}>
          <Search aria-hidden="true" />
          <input
            type="search"
            className="entrada"
            style={{ paddingLeft: 38 }}
            placeholder="Buscar por nombre, SKU o marca"
            value={buscar}
            onChange={(e) => setBuscar(e.target.value)}
            aria-label="Buscar productos"
          />
        </div>
        <select className="entrada" value={categoria} onChange={(e) => setCategoria(e.target.value)} aria-label="Filtrar por categoría">
          <option value="">Todas las categorías</option>
          {CATEGORIAS.map((c) => (
            <option key={c.id} value={c.id}>{c.etiqueta}</option>
          ))}
        </select>
        <select className="entrada" value={estado} onChange={(e) => setEstado(e.target.value)} aria-label="Filtrar por estado">
          <option value="">Todos los estados</option>
          {(Object.keys(ESTADO) as EstadoProducto[]).map((e) => (
            <option key={e} value={e}>{ESTADO[e].texto}</option>
          ))}
        </select>
      </div>

      {error ? (
        <ErrorState mensaje={error} onReintentar={cargar} />
      ) : !lista ? (
        <LoadingSkeleton alto={300} radio={16} />
      ) : (
        <>
          <DataTable
            titulo="Productos"
            vacio="Ningún producto coincide con los filtros."
            filas={filtrados.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA)}
            claveFila={(p) => p.id}
            columnas={[
              {
                clave: "producto",
                titulo: "Producto",
                render: (p) => (
                  <div className="fila" style={{ gap: "var(--s-3)", minWidth: 240 }}>
                    <div className="miniatura" style={{ width: 44, height: 44, borderRadius: 8, display: "grid", placeItems: "center", background: "var(--c-superficie-2)", flex: "none" }}>
                      <ImagenProducto producto={{ ...p, stock: null, enStock: null } as Producto} tamano="mini" />
                    </div>
                    <div>
                      <strong>{p.nombre}</strong>
                      <div className="texto-2" style={{ fontSize: "var(--t-xs)" }}>
                        {p.sku}
                        {p.marca ? ` · ${p.marca}` : ""}
                      </div>
                    </div>
                  </div>
                ),
              },
              { clave: "categoria", titulo: "Categoría", render: (p) => etiquetaCategoria(p.categoria) },
              { clave: "precio", titulo: "Precio", numerica: true, render: (p) => <PriceDisplay monto={p.precio} tamano="sm" /> },
              {
                clave: "estado",
                titulo: "Estado",
                render: (p) => (
                  <div className="pila" style={{ gap: 4 }}>
                    <StatusBadge tono={ESTADO[p.estado].tono}>{ESTADO[p.estado].texto}</StatusBadge>
                    {p.faltantes.length > 0 && (
                      <span className="texto-2" style={{ fontSize: 11, display: "flex", gap: 4, alignItems: "center" }} title={`Falta: ${p.faltantes.join(", ")}`}>
                        <TriangleAlert size={12} color="var(--c-aviso)" /> Faltan {p.faltantes.length} especificaciones
                      </span>
                    )}
                  </div>
                ),
              },
              {
                clave: "acciones",
                titulo: "Acciones",
                render: (p) => (
                  <div className="fila" style={{ gap: 6 }}>
                    <Link href={`/admin/productos/${p.id}`} className="btn btn-secundario btn-sm" aria-label={`Editar ${p.nombre}`}>
                      <Pencil size={14} /> Editar
                    </Link>
                    {SIGUIENTES[p.estado].map((s) => (
                      <button
                        key={s.estado}
                        className={`btn btn-sm ${s.estado === "PUBLICADO" ? "btn-primario" : "btn-fantasma"}`}
                        disabled={s.estado === "PUBLICADO" && p.faltantes.length > 0}
                        title={s.estado === "PUBLICADO" && p.faltantes.length > 0 ? `Falta: ${p.faltantes.join(", ")}` : undefined}
                        onClick={() => setCambio({ p, estado: s.estado, accion: s.accion })}
                      >
                        {s.accion}
                      </button>
                    ))}
                  </div>
                ),
              },
            ]}
          />
          <Pagination pagina={pagina} totalPaginas={Math.ceil(filtrados.length / POR_PAGINA)} onCambiar={setPagina} />
        </>
      )}

      <ConfirmDialog
        abierto={cambio !== null}
        titulo={`¿${cambio?.accion} el producto?`}
        mensaje={
          cambio?.estado === "INACTIVO"
            ? `${cambio.p.nombre} dejará de mostrarse en la tienda y en el configurador.`
            : `${cambio?.p.nombre} se mostrará en la tienda y en el configurador.`
        }
        textoConfirmar={cambio?.accion ?? "Confirmar"}
        peligro={cambio?.estado === "INACTIVO"}
        onCancelar={() => setCambio(null)}
        onConfirmar={() => cambio && aplicar(cambio)}
      />
    </>
  );
}
