"use client";

import { ImageUp, Loader2, Plus, Save, Trash2, TriangleAlert } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useRef, useState } from "react";
import { BASE, CATEGORIAS, ErrorApi, leer, type Categoria, type Producto } from "@/lib/api";
import { ESTADO, SIGUIENTES, type CategoriaSpecs, type EstadoProducto, type ProductoAdmin } from "@/lib/admin";
import { useAuth } from "@/lib/auth";
import { StatusBadge } from "./Badges";
import { ErrorState, LoadingSkeleton } from "./Estados";
import { FormField } from "./FormField";
import { ImagenProducto } from "./ImagenProducto";
import { ConfirmDialog } from "./Modal";
import { useToast } from "./Toast";

interface Extra {
  id: number;
  nombre: string;
  valor: string;
}

const TIPOS_FOTO = "image/jpeg,image/png,image/webp";

/** Crear o editar un producto (HU-05, RF-05) */
export function EditorProducto({ id }: { id?: number }) {
  const { pedirConSesion, token } = useAuth();
  const router = useRouter();
  const toast = useToast();
  const [catalogoSpecs, setCatalogoSpecs] = useState<CategoriaSpecs[] | null>(null);
  const [producto, setProducto] = useState<ProductoAdmin | null>(null);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);

  const [categoria, setCategoria] = useState<Categoria>("PROCESADOR");
  const [sku, setSku] = useState("");
  const [nombre, setNombre] = useState("");
  const [marca, setMarca] = useState("");
  const [precio, setPrecio] = useState("");
  const [valores, setValores] = useState<Record<string, string>>({});
  const [extras, setExtras] = useState<Extra[]>([]);
  const siguienteExtra = useRef(1);

  const [errores, setErrores] = useState<Record<string, string>>({});
  const [general, setGeneral] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [subiendo, setSubiendo] = useState(false);
  const [cambioEstado, setCambioEstado] = useState<{ estado: EstadoProducto; accion: string } | null>(null);

  useEffect(() => {
    pedirConSesion<CategoriaSpecs[]>("/api/catalogo/admin/especificaciones").then(setCatalogoSpecs).catch((e: Error) => setErrorCarga(e.message));
    if (id) {
      pedirConSesion<ProductoAdmin>(`/api/catalogo/admin/productos/${id}`).then(cargarProducto).catch((e: Error) => setErrorCarga(e.message));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const conocidas = useMemo(
    () => catalogoSpecs?.find((c) => c.categoria === categoria)?.especificaciones ?? [],
    [catalogoSpecs, categoria],
  );

  function cargarProducto(p: ProductoAdmin) {
    setProducto(p);
    setCategoria(p.categoria);
    setSku(p.sku);
    setNombre(p.nombre);
    setMarca(p.marca ?? "");
    setPrecio(String(p.precio));
    setValores(p.specs);
    setExtras([]);
  }

  // Las especificaciones guardadas que no son conocidas para la categoría se editan como "otras".
  useEffect(() => {
    if (!producto || !catalogoSpecs) return;
    const claves = new Set((catalogoSpecs.find((c) => c.categoria === producto.categoria)?.especificaciones ?? []).map((s) => s.clave));
    setExtras(
      Object.entries(producto.specs)
        .filter(([k]) => !claves.has(k))
        .map(([nombre, valor]) => ({ id: siguienteExtra.current++, nombre, valor })),
    );
  }, [producto, catalogoSpecs]);

  function especificaciones() {
    const conocidasCargadas = conocidas.filter((s) => (valores[s.clave] ?? "").trim()).map((s) => ({ nombre: s.clave, valor: valores[s.clave].trim() }));
    const otras = extras.filter((e) => e.nombre.trim() || e.valor.trim()).map((e) => ({ nombre: e.nombre.trim(), valor: e.valor.trim() }));
    return [...conocidasCargadas, ...otras];
  }

  async function guardar(e: React.FormEvent) {
    e.preventDefault();
    setGeneral(null);
    setErrores({});
    const n = Number(precio.replace(",", "."));
    if (!precio.trim() || !Number.isFinite(n)) {
      setErrores({ precio: "escribe un número" });
      return;
    }
    setGuardando(true);
    const cuerpo = { categoria, sku: sku.trim(), nombre: nombre.trim(), marca: marca.trim() || null, precio: n, especificaciones: especificaciones() };
    try {
      if (id) {
        const p = await pedirConSesion<ProductoAdmin>(`/api/catalogo/admin/productos/${id}`, { metodo: "PUT", cuerpo });
        cargarProducto(p);
        toast("Cambios guardados");
      } else {
        const p = await pedirConSesion<ProductoAdmin>("/api/catalogo/admin/productos", { metodo: "POST", cuerpo });
        toast("Producto creado como borrador");
        router.replace(`/admin/productos/${p.id}`);
      }
    } catch (err) {
      if (err instanceof ErrorApi && Object.keys(err.campos).length) {
        setErrores(err.campos);
        setGeneral(err.message);
      } else setGeneral(err instanceof Error ? err.message : "No se pudo guardar.");
    } finally {
      setGuardando(false);
    }
  }

  async function subirFoto(archivo: File) {
    if (!id) return;
    if (archivo.size > 2 * 1024 * 1024) {
      toast("La imagen pesa más de 2 MB.", "error");
      return;
    }
    setSubiendo(true);
    try {
      const datos = new FormData();
      datos.append("archivo", archivo);
      const res = await fetch(`${BASE}/api/catalogo/admin/productos/${id}/foto`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: datos,
      });
      cargarProducto(await leer<ProductoAdmin>(res));
      toast("Foto actualizada");
    } catch (err) {
      toast(err instanceof Error ? err.message : "No se pudo subir la foto", "error");
    } finally {
      setSubiendo(false);
    }
  }

  async function aplicarEstado(estado: EstadoProducto) {
    setCambioEstado(null);
    try {
      const p = await pedirConSesion<ProductoAdmin>(`/api/catalogo/admin/productos/${id}/estado`, { metodo: "PATCH", cuerpo: { estado } });
      cargarProducto(p);
      toast(`Producto ${ESTADO[estado].texto.toLowerCase()}`);
    } catch (err) {
      toast(err instanceof Error ? err.message : "No se pudo cambiar el estado", "error");
    }
  }

  if (errorCarga) return <ErrorState mensaje={errorCarga} />;
  if (!catalogoSpecs || (id && !producto)) return <LoadingSkeleton alto={420} radio={16} />;

  const errorSpec = (clave: string) => errores[`especificaciones.${clave}`];
  return (
    <>
      <div className="panel-cabecera">
        <div>
          <Link href="/admin/productos" className="enlace" style={{ fontSize: "var(--t-sm)" }}>← Productos</Link>
          <h1 style={{ marginTop: 6 }}>{id ? producto?.nombre : "Nuevo producto"}</h1>
          {producto && (
            <div className="fila" style={{ marginTop: 6 }}>
              <StatusBadge tono={ESTADO[producto.estado].tono}>{ESTADO[producto.estado].texto}</StatusBadge>
              {producto.estado === "PUBLICADO" && (
                <Link href={`/producto/${producto.id}`} className="enlace" style={{ fontSize: "var(--t-sm)" }} target="_blank">
                  Ver en la tienda
                </Link>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="editor">
        <form className="pila" onSubmit={guardar} noValidate>
          {general && <div className="alerta alerta-error" role="alert">{general}</div>}

          <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-datos">
            <h2 id="t-datos" style={{ fontSize: "var(--t-lg)" }}>Datos del producto</h2>
            <div className="rejilla-campos">
              <FormField etiqueta="Categoría" error={errores.categoria}>
                <select value={categoria} onChange={(e) => setCategoria(e.target.value as Categoria)}>
                  {CATEGORIAS.map((c) => (
                    <option key={c.id} value={c.id}>{c.etiqueta}</option>
                  ))}
                </select>
              </FormField>
              <FormField etiqueta="SKU" error={errores.sku} ayuda="Código único. Se guarda en mayúsculas.">
                <input value={sku} onChange={(e) => setSku(e.target.value)} maxLength={30} />
              </FormField>
            </div>
            <FormField etiqueta="Nombre" error={errores.nombre}>
              <input value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={120} />
            </FormField>
            <div className="rejilla-campos">
              <FormField etiqueta="Marca (opcional)" error={errores.marca}>
                <input value={marca} onChange={(e) => setMarca(e.target.value)} maxLength={40} />
              </FormField>
              <FormField etiqueta="Precio (Bs)" error={errores.precio}>
                <input inputMode="decimal" value={precio} onChange={(e) => setPrecio(e.target.value)} placeholder="0.00" />
              </FormField>
            </div>
          </section>

          <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-specs">
            <div>
              <h2 id="t-specs" style={{ fontSize: "var(--t-lg)" }}>Especificaciones técnicas</h2>
              <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
                Las marcadas con <span className="requerida" /> las usa la verificación de compatibilidad: sin ellas no se puede publicar.
                Las listas se escriben separadas por coma.
              </p>
            </div>
            {conocidas.map((s) => (
              <div className="fila-spec" key={s.clave}>
                <label className="etiqueta-spec" htmlFor={`spec-${s.clave}`}>
                  <span className={s.requerida ? "requerida" : undefined}>{s.etiqueta}</span>
                  <small>{s.clave}{s.unidad ? ` · ${s.unidad}` : ""}{s.tipo === "LISTA" ? " · lista" : ""}</small>
                </label>
                <div className="campo">
                  <input
                    id={`spec-${s.clave}`}
                    className="entrada"
                    inputMode={s.tipo === "NUMERO" ? "decimal" : undefined}
                    placeholder={s.tipo === "LISTA" ? "Valor 1, Valor 2" : s.tipo === "NUMERO" ? "Número" : ""}
                    value={valores[s.clave] ?? ""}
                    aria-invalid={errorSpec(s.clave) ? true : undefined}
                    onChange={(e) => setValores({ ...valores, [s.clave]: e.target.value })}
                    maxLength={80}
                  />
                  {errorSpec(s.clave) && <span className="error">{errorSpec(s.clave)}</span>}
                </div>
                <span />
              </div>
            ))}

            <h3 style={{ fontSize: "var(--t-md)", marginTop: "var(--s-2)" }}>Otras especificaciones</h3>
            {extras.length === 0 && <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>Ninguna.</p>}
            {extras.map((x, i) => (
              <div className="fila-spec" key={x.id}>
                <div className="campo">
                  <input
                    className="entrada"
                    placeholder="nombre (ej.: color)"
                    aria-label="Nombre de la especificación"
                    value={x.nombre}
                    aria-invalid={errores[`especificaciones[${conocidas.length + i}].nombre`] ? true : undefined}
                    onChange={(e) => setExtras(extras.map((y) => (y.id === x.id ? { ...y, nombre: e.target.value.toLowerCase() } : y)))}
                    maxLength={40}
                  />
                  {errorSpec(x.nombre) && <span className="error">{errorSpec(x.nombre)}</span>}
                </div>
                <input
                  className="entrada"
                  placeholder="valor"
                  aria-label="Valor de la especificación"
                  value={x.valor}
                  onChange={(e) => setExtras(extras.map((y) => (y.id === x.id ? { ...y, valor: e.target.value } : y)))}
                  maxLength={80}
                />
                <button type="button" className="btn btn-fantasma btn-icono" onClick={() => setExtras(extras.filter((y) => y.id !== x.id))} aria-label="Quitar especificación">
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
            {Object.keys(errores).some((k) => k.startsWith("especificaciones[")) && (
              <span className="error" style={{ fontSize: "var(--t-xs)", color: "var(--c-error)" }}>
                Los nombres usan minúsculas, números y guion bajo (ej.: color_led).
              </span>
            )}
            <button
              type="button"
              className="btn btn-secundario btn-sm"
              style={{ alignSelf: "flex-start" }}
              onClick={() => setExtras([...extras, { id: siguienteExtra.current++, nombre: "", valor: "" }])}
            >
              <Plus size={15} /> Agregar especificación
            </button>
          </section>

          <div className="fila">
            <button className="btn btn-primario btn-lg" disabled={guardando}>
              {guardando ? <Loader2 size={18} className="girar" /> : <Save size={18} />}
              {id ? "Guardar cambios" : "Crear como borrador"}
            </button>
            <Link href="/admin/productos" className="btn btn-fantasma">Cancelar</Link>
          </div>
        </form>

        <aside className="pila">
          <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-foto">
            <h2 id="t-foto" style={{ fontSize: "var(--t-lg)" }}>Foto</h2>
            <div className="foto-editor">
              {producto ? <ImagenProducto producto={{ ...producto, stock: null, enStock: null } as Producto} tamano="grande" /> : <ImageUp size={40} />}
            </div>
            {id ? (
              <label className="btn btn-secundario btn-bloque" style={{ cursor: subiendo ? "wait" : "pointer" }}>
                {subiendo ? <Loader2 size={16} className="girar" /> : <ImageUp size={16} />}
                {subiendo ? "Subiendo…" : producto?.fotoUrl ? "Cambiar foto" : "Subir foto"}
                <input
                  type="file"
                  accept={TIPOS_FOTO}
                  className="sr-only"
                  disabled={subiendo}
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    e.target.value = "";
                    if (f) subirFoto(f);
                  }}
                />
              </label>
            ) : (
              <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>Guarda el producto para poder subir su foto.</p>
            )}
            <p className="texto-2" style={{ fontSize: "var(--t-xs)" }}>JPG, PNG o WEBP, hasta 2 MB.</p>
          </section>

          {producto && (
            <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-estado">
              <h2 id="t-estado" style={{ fontSize: "var(--t-lg)" }}>Publicación</h2>
              {producto.faltantes.length > 0 && (
                <div className="alerta alerta-aviso">
                  <TriangleAlert aria-hidden="true" />
                  <div>Para publicarlo falta: {producto.faltantes.join(", ")}.</div>
                </div>
              )}
              {SIGUIENTES[producto.estado].map((s) => (
                <button
                  key={s.estado}
                  className={`btn btn-bloque ${s.estado === "PUBLICADO" ? "btn-primario" : "btn-secundario"}`}
                  disabled={s.estado === "PUBLICADO" && producto.faltantes.length > 0}
                  onClick={() => setCambioEstado(s)}
                >
                  {s.accion}
                </button>
              ))}
            </section>
          )}
        </aside>
      </div>

      <ConfirmDialog
        abierto={cambioEstado !== null}
        titulo={`¿${cambioEstado?.accion} el producto?`}
        mensaje={cambioEstado?.estado === "INACTIVO" ? "Dejará de mostrarse en la tienda y en el configurador." : "Se mostrará en la tienda y en el configurador."}
        textoConfirmar={cambioEstado?.accion ?? "Confirmar"}
        peligro={cambioEstado?.estado === "INACTIVO"}
        onCancelar={() => setCambioEstado(null)}
        onConfirmar={() => cambioEstado && aplicarEstado(cambioEstado.estado)}
      />
    </>
  );
}
