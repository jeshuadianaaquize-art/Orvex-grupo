"use client";

import { ChevronLeft, ChevronRight, Save } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ComponentSelector } from "@/components/ComponentSelector";
import { EmptyState, ErrorState, LoadingSkeleton } from "@/components/Estados";
import { FormField } from "@/components/FormField";
import { IconoCategoria } from "@/components/iconos";
import { Modal } from "@/components/Modal";
import { OptimizationPanel } from "@/components/OptimizationPanel";
import { PcSummary } from "@/components/PcSummary";
import { useToast } from "@/components/Toast";
import {
  CATEGORIAS,
  ErrorApi,
  USOS,
  analizarOptimizacion,
  listarProductos,
  verificarArmado,
  type Alternativa,
  type AnalisisOptimizacion,
  type Categoria,
  type DetalleConfiguracion,
  type Producto,
  type Reporte,
  type UsoPrevisto,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useCart } from "@/lib/cart";

/** Si hay que ingresar para guardar, la selección se conserva aquí y se recupera al volver. */
const PENDIENTE = "orvex-armado-pendiente";

interface Guardada {
  id: number;
  nombre: string;
  uso: UsoPrevisto | null;
  /** Piezas tal como están guardadas */
  productoIds: number[];
}

const mismas = (a: number[], b: number[]) => a.length === b.length && [...a].sort().join() === [...b].sort().join();

export default function ArmadorPagina() {
  return (
    <Suspense fallback={null}>
      <Armador />
    </Suspense>
  );
}

function Armador() {
  const params = useSearchParams();
  const [productos, setProductos] = useState<Producto[] | null>(null);
  const [errorCarga, setErrorCarga] = useState<string | null>(null);
  const [seleccion, setSeleccion] = useState<Partial<Record<Categoria, number>>>({});
  const [paso, setPaso] = useState(0);
  const [reporte, setReporte] = useState<Reporte | null>(null);
  const [desactualizado, setDesactualizado] = useState(false);
  const [verificando, setVerificando] = useState(false);
  const [agregado, setAgregado] = useState(false);
  // Cuenta las verificaciones: si la selección cambia a mitad de camino, la respuesta vieja se descarta.
  const solicitud = useRef(0);
  const { agregarVarios, agregarConfiguracion } = useCart();
  const { usuario, listo, pedirConSesion } = useAuth();
  const router = useRouter();
  const toast = useToast();
  // Configuración guardada que se está editando (si se abrió desde "Mis configuraciones").
  const [guardada, setGuardada] = useState<Guardada | null>(null);
  const [dialogo, setDialogo] = useState(false);
  // Optimización (M05)
  const [uso, setUso] = useState<UsoPrevisto>("JUEGOS");
  const [analisis, setAnalisis] = useState<AnalisisOptimizacion | null>(null);
  const [analisisViejo, setAnalisisViejo] = useState(false);
  const [analizando, setAnalizando] = useState(false);
  const [analisisGuardado, setAnalisisGuardado] = useState(false);
  const pedidoOpt = useRef(0);

  const cargar = useCallback(() => {
    setErrorCarga(null);
    setProductos(null);
    listarProductos()
      .then(setProductos)
      .catch((e: Error) => setErrorCarga(e.message));
  }, []);
  useEffect(cargar, [cargar]);

  // RF-06: "partir de un componente suelto"
  const desde = useRef(params.get("producto"));
  useEffect(() => {
    if (!productos || !desde.current) return;
    const p = productos.find((x) => x.id === Number(desde.current));
    desde.current = null;
    if (!p) return;
    setSeleccion({ [p.categoria]: p.id });
    const i = CATEGORIAS.findIndex((c) => c.id === p.categoria);
    setPaso(i === 0 ? 1 : 0);
    toast(`Partimos de ${p.nombre}. Elige el resto de las piezas.`, "info");
  }, [productos, toast]);

  // /armador?config=ID abre una configuración guardada del usuario
  const configPedida = useRef(params.get("config"));
  useEffect(() => {
    if (!productos || !listo) return;
    const id = configPedida.current;
    if (id) {
      configPedida.current = null;
      if (!usuario) return;
      pedirConSesion<DetalleConfiguracion>(`/api/configuraciones/${encodeURIComponent(id)}`)
        .then((d) => {
          const mapa = new Map(productos.map((p) => [p.id, p]));
          const nueva: Partial<Record<Categoria, number>> = {};
          let faltan = 0;
          for (const pid of d.productoIds) {
            const p = mapa.get(pid);
            if (p) nueva[p.categoria] = p.id;
            else faltan++;
          }
          setSeleccion(nueva);
          setGuardada({ id: d.configuracion.id, nombre: d.configuracion.nombre, uso: d.configuracion.uso, productoIds: d.productoIds });
          if (d.configuracion.uso) setUso(d.configuracion.uso);
          setReporte(faltan === 0 && d.verificacionActual ? d.verificacionActual.reporte : null);
          setDesactualizado(false);
          toast(
            faltan
              ? `Se abrió «${d.configuracion.nombre}», pero ${faltan} pieza${faltan === 1 ? " ya no está" : "s ya no están"} a la venta.`
              : `Se abrió «${d.configuracion.nombre}»`,
            faltan ? "error" : "info",
          );
        })
        .catch((e: Error) => toast(e.message, "error"));
      return;
    }
    if (desde.current) return;
    try {
      const pendiente = sessionStorage.getItem(PENDIENTE);
      if (!pendiente) return;
      sessionStorage.removeItem(PENDIENTE);
      const ids = JSON.parse(pendiente) as number[];
      const nueva: Partial<Record<Categoria, number>> = {};
      for (const p of productos) if (ids.includes(p.id)) nueva[p.categoria] = p.id;
      setSeleccion(nueva);
      if (usuario) setDialogo(true);
    } catch {
      /* sin almacenamiento: se empieza de cero */
    }
  }, [productos, listo, usuario, pedirConSesion, toast]);

  const porId = useMemo(() => new Map((productos ?? []).map((p) => [p.id, p])), [productos]);
  const elegidos = useMemo(() => {
    const r: Partial<Record<Categoria, Producto>> = {};
    for (const [cat, id] of Object.entries(seleccion)) {
      const p = id !== undefined ? porId.get(id) : undefined;
      if (p) r[cat as Categoria] = p;
    }
    return r;
  }, [seleccion, porId]);

  const actual = CATEGORIAS[paso];
  const opciones = useMemo(
    () => (productos ?? []).filter((p) => p.categoria === actual.id).sort((a, b) => a.precio - b.precio),
    [productos, actual.id],
  );

  function elegir(categoria: Categoria, id: number | undefined) {
    solicitud.current++;
    setVerificando(false);
    setAgregado(false);
    if (reporte) setDesactualizado(true);
    pedidoOpt.current++;
    setAnalizando(false);
    if (analisis) setAnalisisViejo(true);
    setSeleccion((s) => ({ ...s, [categoria]: id }));
    // Al elegir, avanza a la siguiente categoría que falte (sin saltarse si se está quitando).
    if (id !== undefined && paso < CATEGORIAS.length - 1) setTimeout(() => setPaso((p) => Math.min(p + 1, CATEGORIAS.length - 1)), 180);
  }

  async function verificar() {
    const ids = Object.values(elegidos).map((p) => p!.id);
    const n = ++solicitud.current;
    setVerificando(true);
    try {
      const r = await verificarArmado(ids);
      if (n !== solicitud.current) return;
      setReporte(r.reporte);
      setDesactualizado(false);
    } catch (e) {
      if (n === solicitud.current) toast((e as Error).message, "error");
    } finally {
      if (n === solicitud.current) setVerificando(false);
    }
  }

  const idsElegidos = useMemo(() => Object.values(elegidos).map((p) => p!.id), [elegidos]);

  async function analizar(ids: number[] = idsElegidos, usoElegido: UsoPrevisto = uso) {
    const n = ++pedidoOpt.current;
    setAnalizando(true);
    // Si es la configuración abierta sin cambios, el análisis se guarda con ella
    const enCuenta = !!(usuario && guardada && mismas(ids, guardada.productoIds));
    try {
      const a = enCuenta
        ? await pedirConSesion<AnalisisOptimizacion>(`/api/optimizacion/configuraciones/${guardada!.id}`, {
            metodo: "POST",
            cuerpo: { uso: usoElegido },
          })
        : await analizarOptimizacion(ids, usoElegido);
      if (n !== pedidoOpt.current) return;
      setAnalisis(a);
      setAnalisisViejo(false);
      setAnalisisGuardado(enCuenta);
    } catch (e) {
      if (n === pedidoOpt.current) toast((e as Error).message, "error");
    } finally {
      if (n === pedidoOpt.current) setAnalizando(false);
    }
  }

  function cambiarUso(u: UsoPrevisto) {
    setUso(u);
    pedidoOpt.current++;
    setAnalizando(false);
    if (analisis) setAnalisisViejo(true);
  }

  /** RF-10: aplicar una alternativa cambia una sola pieza y vuelve a analizar. */
  function reemplazar(a: Alternativa) {
    const ids = [...idsElegidos.filter((id) => porId.get(id)?.categoria !== a.categoria), a.producto.id];
    elegir(a.categoria, a.producto.id);
    toast(`${a.producto.nombre} quedó en tu armado`);
    void analizar(ids);
  }

  function pedirGuardar() {
    if (usuario) {
      setDialogo(true);
      return;
    }
    try {
      sessionStorage.setItem(PENDIENTE, JSON.stringify(Object.values(elegidos).map((p) => p!.id)));
    } catch {
      /* sin almacenamiento: al volver habrá que elegir de nuevo */
    }
    router.push(`/ingresar?siguiente=${encodeURIComponent("/armador")}`);
  }

  /** Con la configuración guardada abierta y sin cambios, entra como una PC completa */
  async function agregarAlCarrito() {
    const comoPc = !!(usuario && guardada && mismas(idsElegidos, guardada.productoIds));
    const ok = comoPc ? await agregarConfiguracion(guardada!.id) : await agregarVarios(Object.values(elegidos) as Producto[]);
    if (!ok) return;
    setAgregado(true);
    toast(comoPc ? `«${guardada!.nombre}» se agregó al carrito como PC completa` : "El armado se agregó al carrito");
  }

  return (
    <div className="tema-oscuro armador">
      <div className="contenedor">
        <header style={{ marginBottom: "var(--s-6)" }}>
          <span className="eyebrow" style={{ color: "var(--c-acento)" }}>
            Configurador de PC
          </span>
          <h1 style={{ fontSize: "var(--t-2xl)", marginTop: 6 }}>Arma tu PC paso a paso</h1>
          <p className="texto-2">Elige una pieza por categoría. Puedes saltarte las que no necesites.</p>
        </header>

        {errorCarga ? (
          <ErrorState mensaje={`No se pudo cargar el catálogo. ${errorCarga}`} onReintentar={cargar} />
        ) : (
          <div className="armador-rejilla">
            <nav className="armador-pasos" aria-label="Categorías del armado">
              {CATEGORIAS.map((c, i) => {
                const p = elegidos[c.id];
                return (
                  <button
                    key={c.id}
                    className={`paso ${p ? "hecho" : ""}`}
                    aria-current={i === paso ? "step" : undefined}
                    onClick={() => setPaso(i)}
                  >
                    <span className="paso-icono">
                      <IconoCategoria categoria={c.id} size={18} />
                    </span>
                    <span>
                      {c.etiqueta}
                      <small>{p ? p.nombre : "Sin elegir"}</small>
                    </span>
                  </button>
                );
              })}
            </nav>

            <section aria-labelledby="titulo-paso">
              <div className="fila" style={{ justifyContent: "space-between", marginBottom: "var(--s-4)" }}>
                <div>
                  <span className="eyebrow" style={{ color: "var(--c-texto-2)" }}>
                    Paso {paso + 1} de {CATEGORIAS.length}
                  </span>
                  <h2 id="titulo-paso" style={{ fontSize: "var(--t-xl)" }}>
                    {actual.etiqueta}
                  </h2>
                </div>
                <div className="fila">
                  <button className="btn btn-secundario btn-icono" disabled={paso === 0} onClick={() => setPaso(paso - 1)} aria-label="Categoría anterior">
                    <ChevronLeft size={18} />
                  </button>
                  <button
                    className="btn btn-secundario btn-icono"
                    disabled={paso === CATEGORIAS.length - 1}
                    onClick={() => setPaso(paso + 1)}
                    aria-label="Categoría siguiente"
                  >
                    <ChevronRight size={18} />
                  </button>
                </div>
              </div>

              {!productos ? (
                <div className="pila">
                  {Array.from({ length: 4 }, (_, i) => (
                    <LoadingSkeleton key={i} alto={96} radio={16} />
                  ))}
                </div>
              ) : opciones.length === 0 ? (
                <EmptyState titulo="No hay productos en esta categoría" mensaje="Puedes continuar con las demás categorías." />
              ) : (
                <ComponentSelector
                  key={actual.id}
                  productos={opciones}
                  seleccionado={seleccion[actual.id]}
                  onElegir={(id) => elegir(actual.id, id)}
                />
              )}
            </section>

            <PcSummary
              seleccion={elegidos}
              reporte={reporte}
              verificando={verificando}
              desactualizado={desactualizado}
              onVerificar={verificar}
              onAgregar={agregarAlCarrito}
              agregado={agregado}
              extra={
                <div className="pila" style={{ gap: "var(--s-2)" }}>
                  {guardada && (
                    <p className="texto-2" style={{ fontSize: "var(--t-xs)", textAlign: "center" }}>
                      Editando «{guardada.nombre}»
                    </p>
                  )}
                  <button className="btn btn-fantasma btn-bloque" disabled={Object.keys(elegidos).length < 2} onClick={pedirGuardar}>
                    <Save size={16} />
                    {guardada ? "Guardar cambios" : usuario ? "Guardar configuración" : "Ingresa para guardar"}
                  </button>
                </div>
              }
            />
          </div>
        )}
        {!errorCarga && (
          <OptimizationPanel
            uso={uso}
            onUso={cambiarUso}
            analisis={analisis}
            cargando={analizando}
            desactualizado={analisisViejo}
            puedeAnalizar={!!elegidos.PROCESADOR}
            guardaEnCuenta={analisisGuardado}
            onAnalizar={() => void analizar()}
            onReemplazar={reemplazar}
          />
        )}
      </div>
      <DialogoGuardar
        abierto={dialogo}
        guardada={guardada}
        productoIds={Object.values(elegidos).map((p) => p!.id)}
        onCerrar={() => setDialogo(false)}
        onGuardada={(g, reporteNuevo) => {
          setDialogo(false);
          setGuardada(g);
          if (g.uso) setUso(g.uso);
          if (reporteNuevo) {
            solicitud.current++;
            setVerificando(false);
            setReporte(reporteNuevo);
            setDesactualizado(false);
          }
          router.replace(`/armador?config=${g.id}`, { scroll: false });
        }}
      />
    </div>
  );
}

/** Nombre y uso previsto de la configuración (tabla configuracion_pc). */
function DialogoGuardar({
  abierto,
  guardada,
  productoIds,
  onCerrar,
  onGuardada,
}: {
  abierto: boolean;
  guardada: Guardada | null;
  productoIds: number[];
  onCerrar: () => void;
  onGuardada: (g: Guardada, reporte: Reporte | null) => void;
}) {
  const { pedirConSesion } = useAuth();
  const toast = useToast();
  const [nombre, setNombre] = useState("");
  const [uso, setUso] = useState<UsoPrevisto | "">("");
  const [comoNueva, setComoNueva] = useState(false);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (!abierto) return;
    setNombre(guardada?.nombre ?? "Mi PC");
    setUso(guardada?.uso ?? "");
    setComoNueva(false);
    setErrores({});
  }, [abierto, guardada]);

  async function guardar(e: React.FormEvent) {
    e.preventDefault();
    setGuardando(true);
    setErrores({});
    const actualizar = guardada !== null && !comoNueva;
    try {
      const d = await pedirConSesion<DetalleConfiguracion>(
        actualizar ? `/api/configuraciones/${guardada!.id}` : "/api/configuraciones",
        { metodo: actualizar ? "PUT" : "POST", cuerpo: { nombre: nombre.trim(), uso: uso || null, productoIds } },
      );
      toast(actualizar ? "Configuración actualizada" : "Configuración guardada en tu cuenta");
      onGuardada(
        { id: d.configuracion.id, nombre: d.configuracion.nombre, uso: d.configuracion.uso, productoIds: d.productoIds },
        d.verificacionActual?.reporte ?? null,
      );
    } catch (err) {
      if (err instanceof ErrorApi && Object.keys(err.campos).length) setErrores(err.campos);
      else toast(err instanceof Error ? err.message : "No se pudo guardar", "error");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Modal abierto={abierto} titulo={guardada ? "Guardar cambios" : "Guardar configuración"} onCerrar={onCerrar}>
      <form className="pila" onSubmit={guardar} noValidate style={{ color: "var(--c-texto)" }}>
        <FormField etiqueta="Nombre" error={errores.nombre} ayuda="Para reconocerla en «Mi cuenta».">
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={80} required />
        </FormField>
        <FormField etiqueta="Uso previsto (opcional)" error={errores.uso} ayuda="Lo usará el optimizador para sugerir mejoras.">
          <select value={uso} onChange={(e) => setUso(e.target.value as UsoPrevisto | "")}>
            <option value="">Sin indicar</option>
            {USOS.map((u) => (
              <option key={u.id} value={u.id}>
                {u.etiqueta}
              </option>
            ))}
          </select>
        </FormField>
        {errores.productoIds && <p className="error">{errores.productoIds}</p>}
        {guardada && (
          <label className="fila" style={{ fontSize: "var(--t-sm)", gap: 8 }}>
            <input type="checkbox" checked={comoNueva} onChange={(e) => setComoNueva(e.target.checked)} />
            Guardar como una configuración nueva
          </label>
        )}
        <div className="fila" style={{ justifyContent: "flex-end" }}>
          <button type="button" className="btn btn-secundario" onClick={onCerrar}>
            Cancelar
          </button>
          <button className="btn btn-primario" disabled={guardando || !nombre.trim()}>
            {guardando ? "Guardando…" : "Guardar"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
