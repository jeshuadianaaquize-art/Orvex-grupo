"use client";

import { Pencil, Plus, Trash2 } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { DataTable } from "@/components/DataTable";
import { ErrorState, LoadingSkeleton } from "@/components/Estados";
import { FormField } from "@/components/FormField";
import { ConfirmDialog, Modal } from "@/components/Modal";
import { useToast } from "@/components/Toast";
import { CATEGORIAS, ErrorApi, etiquetaCategoria, type Categoria } from "@/lib/api";
import { useAuth } from "@/lib/auth";

/** Fila de la tabla regla_compatibilidad (RNF-06: las reglas se cambian sin tocar el código). */
interface Regla {
  id: number;
  categoriaA: Categoria;
  categoriaB: Categoria;
  nombre: string;
  condicion: string;
}

type Form = Omit<Regla, "id">;

const VACIA: Form = { categoriaA: "PROCESADOR", categoriaB: "PLACA_MADRE", nombre: "", condicion: "" };

export default function AdminReglas() {
  const { pedirConSesion } = useAuth();
  const toast = useToast();
  const [reglas, setReglas] = useState<Regla[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editando, setEditando] = useState<Regla | "nueva" | null>(null);
  const [form, setForm] = useState<Form>(VACIA);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [guardando, setGuardando] = useState(false);
  const [borrar, setBorrar] = useState<Regla | null>(null);

  const cargar = useCallback(() => {
    setError(null);
    pedirConSesion<Regla[]>("/api/reglas")
      .then(setReglas)
      .catch((e: Error) => setError(e.message));
  }, [pedirConSesion]);
  useEffect(cargar, [cargar]);

  function abrir(r: Regla | "nueva") {
    setErrores({});
    setForm(r === "nueva" ? VACIA : { categoriaA: r.categoriaA, categoriaB: r.categoriaB, nombre: r.nombre, condicion: r.condicion });
    setEditando(r);
  }

  async function guardar(e: React.FormEvent) {
    e.preventDefault();
    if (!editando) return;
    setGuardando(true);
    setErrores({});
    try {
      if (editando === "nueva") {
        await pedirConSesion("/api/reglas", { metodo: "POST", cuerpo: form });
        toast("Regla creada");
      } else {
        await pedirConSesion(`/api/reglas/${editando.id}`, { metodo: "PUT", cuerpo: form });
        toast("Regla actualizada");
      }
      setEditando(null);
      cargar();
    } catch (err) {
      if (err instanceof ErrorApi && Object.keys(err.campos).length) setErrores(err.campos);
      else toast(err instanceof Error ? err.message : "No se pudo guardar", "error");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <>
      <div className="panel-cabecera">
        <div>
          <span className="eyebrow">Configurador</span>
          <h1>Reglas de compatibilidad</h1>
          <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
            El armador aplica estas reglas a cada verificación. Los cambios rigen en menos de un minuto.
          </p>
        </div>
        <button className="btn btn-primario" onClick={() => abrir("nueva")} disabled={!reglas}>
          <Plus size={16} /> Nueva regla
        </button>
      </div>

      {error ? (
        <ErrorState mensaje={error} onReintentar={cargar} />
      ) : !reglas ? (
        <LoadingSkeleton alto={240} radio={16} />
      ) : (
        <DataTable
          titulo="Reglas de compatibilidad"
          vacio="No hay reglas: el armador no puede verificar nada."
          filas={reglas}
          claveFila={(r) => r.id}
          columnas={[
            { clave: "nombre", titulo: "Regla", render: (r) => <strong>{r.nombre}</strong> },
            {
              clave: "entre",
              titulo: "Compara",
              render: (r) => (
                <span style={{ whiteSpace: "nowrap" }}>
                  {etiquetaCategoria(r.categoriaA)} → {etiquetaCategoria(r.categoriaB)}
                </span>
              ),
            },
            { clave: "condicion", titulo: "Condición", render: (r) => <code className="codigo">{r.condicion}</code> },
            {
              clave: "acciones",
              titulo: "Acciones",
              render: (r) => (
                <div className="fila" style={{ gap: 2 }}>
                  <button className="btn btn-fantasma btn-icono" onClick={() => abrir(r)} aria-label={`Editar ${r.nombre}`}>
                    <Pencil size={16} />
                  </button>
                  <button className="btn btn-fantasma btn-icono" onClick={() => setBorrar(r)} aria-label={`Eliminar ${r.nombre}`}>
                    <Trash2 size={16} />
                  </button>
                </div>
              ),
            },
          ]}
        />
      )}

      <Modal abierto={editando !== null} titulo={editando === "nueva" ? "Nueva regla" : "Editar regla"} onCerrar={() => setEditando(null)}>
        <form className="pila" onSubmit={guardar} noValidate style={{ color: "var(--c-texto)" }}>
          <FormField etiqueta="Nombre" error={errores.nombre} ayuda="Se muestra al cliente en el resultado de la verificación.">
            <input value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} maxLength={80} />
          </FormField>
          <div className="rejilla-2">
            <FormField etiqueta="Componente «a»" error={errores.categoriaA}>
              <select value={form.categoriaA} onChange={(e) => setForm({ ...form, categoriaA: e.target.value as Categoria })}>
                {CATEGORIAS.map((c) => (
                  <option key={c.id} value={c.id}>{c.etiqueta}</option>
                ))}
              </select>
            </FormField>
            <FormField etiqueta="Componente «b»">
              <select value={form.categoriaB} onChange={(e) => setForm({ ...form, categoriaB: e.target.value as Categoria })}>
                {CATEGORIAS.map((c) => (
                  <option key={c.id} value={c.id}>{c.etiqueta}</option>
                ))}
              </select>
            </FormField>
          </div>
          <FormField
            etiqueta="Condición"
            error={errores.condicion}
            ayuda="a.dato operador b.dato o un número. Operadores: = < <= > >= contiene en. Agrega [advertencia] para que no bloquee la compra."
          >
            <input
              value={form.condicion}
              onChange={(e) => setForm({ ...form, condicion: e.target.value })}
              maxLength={255}
              spellCheck={false}
              placeholder="a.socket = b.socket"
              style={{ fontFamily: "ui-monospace, Consolas, monospace" }}
            />
          </FormField>
          <details className="texto-2" style={{ fontSize: "var(--t-xs)" }}>
            <summary>Ejemplos</summary>
            <ul style={{ margin: "6px 0 0 18px" }}>
              <li><code>a.tipo_ram = b.tipo_ram</code> — deben coincidir</li>
              <li><code>a.largo_mm &lt;= b.max_largo_gpu_mm</code> — que quepa</li>
              <li><code>a.factores_forma contiene b.factor_forma</code> — la lista de «a» incluye el valor de «b»</li>
              <li><code>a.interfaz en b.interfaces_almacenamiento</code> — el valor está en la lista</li>
              <li><code>a.frecuencia_mhz &lt;= b.frecuencia_max_mhz [advertencia]</code> — solo avisa</li>
              <li><code>consumo(base=100) * 1.30 &lt;= b.potencia_w</code> — consumo total contra la fuente</li>
            </ul>
          </details>
          <div className="fila" style={{ justifyContent: "flex-end" }}>
            <button type="button" className="btn btn-secundario" onClick={() => setEditando(null)}>
              Cancelar
            </button>
            <button className="btn btn-primario" disabled={guardando}>
              {guardando ? "Guardando…" : "Guardar"}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        abierto={borrar !== null}
        titulo="¿Eliminar la regla?"
        mensaje={borrar ? `«${borrar.nombre}» dejará de aplicarse en el armador.` : ""}
        textoConfirmar="Eliminar"
        peligro
        onCancelar={() => setBorrar(null)}
        onConfirmar={async () => {
          const r = borrar;
          setBorrar(null);
          if (!r) return;
          try {
            await pedirConSesion(`/api/reglas/${r.id}`, { metodo: "DELETE" });
            toast("Regla eliminada", "info");
            cargar();
          } catch (err) {
            toast(err instanceof Error ? err.message : "No se pudo eliminar", "error");
          }
        }}
      />
    </>
  );
}
