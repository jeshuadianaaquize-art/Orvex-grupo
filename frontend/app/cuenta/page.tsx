"use client";

import { Cpu, LogOut, MapPin, Package, Pencil, Plus, Save, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { CompatibilityBadge, StatusBadge } from "@/components/Badges";
import { EmptyState, ErrorState, LoadingSkeleton } from "@/components/Estados";
import { FormField } from "@/components/FormField";
import { ConfirmDialog, Modal } from "@/components/Modal";
import { RequiereSesion } from "@/components/RequiereSesion";
import { useToast } from "@/components/Toast";
import { ErrorApi, USOS, bs, fechaUtc, type ResumenConfiguracion } from "@/lib/api";
import { ETIQUETA_ROL, useAuth, type Usuario } from "@/lib/auth";

interface Direccion {
  id: number;
  calle: string;
  ciudad: string;
  referencia: string | null;
}

const DIR_VACIA = { calle: "", ciudad: "", referencia: "" };

export default function CuentaPagina() {
  return (
    <RequiereSesion>
      <Cuenta />
    </RequiereSesion>
  );
}

function Cuenta() {
  const { usuario, salir } = useAuth();
  const router = useRouter();
  const [confirmarSalida, setConfirmarSalida] = useState(false);
  if (!usuario) return null;

  return (
    <div className="contenedor">
      <div className="panel-cabecera" style={{ marginTop: "var(--s-8)", marginBottom: 0 }}>
        <div>
          <span className="eyebrow">Mi cuenta</span>
          <h1 style={{ fontSize: "var(--t-2xl)" }}>Hola, {usuario.nombre.split(" ")[0]}</h1>
          <div className="fila" style={{ marginTop: 6 }}>
            <StatusBadge tono="info">{ETIQUETA_ROL[usuario.rol]}</StatusBadge>
            <span className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
              Miembro desde {new Date(usuario.fechaRegistro).toLocaleDateString("es-BO", { month: "long", year: "numeric" })}
            </span>
          </div>
        </div>
        <div className="fila" style={{ flexWrap: "wrap" }}>
          <Link href="/pedidos" className="btn btn-secundario">
            <Package size={16} /> Mis pedidos
          </Link>
          <button className="btn btn-secundario" onClick={() => setConfirmarSalida(true)}>
            <LogOut size={16} /> Cerrar sesión
          </button>
        </div>
      </div>
      <div className="cuenta-rejilla">
        <div className="pila">
          <DatosPersonales />
          <CambiarContrasena />
        </div>
        <div className="pila">
          <MisConfiguraciones />
          <Direcciones />
          <MisAvisos />
        </div>
      </div>
      <ConfirmDialog
        abierto={confirmarSalida}
        titulo="¿Cerrar sesión?"
        mensaje="Tendrás que volver a ingresar para ver tu cuenta y tus pedidos."
        textoConfirmar="Cerrar sesión"
        onCancelar={() => setConfirmarSalida(false)}
        onConfirmar={async () => {
          setConfirmarSalida(false);
          await salir();
          router.replace("/");
        }}
      />
    </div>
  );
}

function DatosPersonales() {
  const { usuario, pedirConSesion, actualizarUsuario } = useAuth();
  const toast = useToast();
  const [nombre, setNombre] = useState(usuario?.nombre ?? "");
  const [telefono, setTelefono] = useState(usuario?.telefono ?? "");
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [guardando, setGuardando] = useState(false);
  const cambio = nombre !== usuario?.nombre || telefono !== (usuario?.telefono ?? "");

  async function guardar(e: React.FormEvent) {
    e.preventDefault();
    setGuardando(true);
    setErrores({});
    try {
      const u = await pedirConSesion<Usuario>("/api/usuarios/me", { metodo: "PUT", cuerpo: { nombre, telefono } });
      actualizarUsuario(u);
      toast("Datos guardados");
    } catch (err) {
      if (err instanceof ErrorApi && Object.keys(err.campos).length) setErrores(err.campos);
      else toast(err instanceof Error ? err.message : "No se pudo guardar", "error");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-datos">
      <h2 id="t-datos" style={{ fontSize: "var(--t-lg)" }}>Datos personales</h2>
      <form className="pila" onSubmit={guardar} noValidate>
        <FormField etiqueta="Nombre completo" error={errores.nombre}>
          <input value={nombre} onChange={(e) => setNombre(e.target.value)} maxLength={80} autoComplete="name" />
        </FormField>
        <FormField etiqueta="Correo electrónico" ayuda="El correo no se puede cambiar: es tu usuario para ingresar.">
          <input value={usuario?.correo ?? ""} disabled readOnly />
        </FormField>
        <FormField etiqueta="Teléfono" error={errores.telefono}>
          <input type="tel" value={telefono} onChange={(e) => setTelefono(e.target.value)} maxLength={20} autoComplete="tel" />
        </FormField>
        <button className="btn btn-primario" style={{ alignSelf: "flex-start" }} disabled={!cambio || guardando}>
          <Save size={16} /> {guardando ? "Guardando…" : "Guardar cambios"}
        </button>
      </form>
    </section>
  );
}

function CambiarContrasena() {
  const { pedirConSesion } = useAuth();
  const toast = useToast();
  const [actual, setActual] = useState("");
  const [nueva, setNueva] = useState("");
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [guardando, setGuardando] = useState(false);

  async function guardar(e: React.FormEvent) {
    e.preventDefault();
    setGuardando(true);
    setErrores({});
    try {
      await pedirConSesion("/api/usuarios/me/contrasena", { metodo: "POST", cuerpo: { actual, nueva } });
      setActual("");
      setNueva("");
      toast("Contraseña actualizada");
    } catch (err) {
      if (err instanceof ErrorApi && Object.keys(err.campos).length) setErrores(err.campos);
      else toast(err instanceof Error ? err.message : "No se pudo cambiar", "error");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-clave">
      <h2 id="t-clave" style={{ fontSize: "var(--t-lg)" }}>Contraseña</h2>
      <form className="pila" onSubmit={guardar} noValidate>
        <FormField etiqueta="Contraseña actual" error={errores.actual}>
          <input type="password" value={actual} onChange={(e) => setActual(e.target.value)} autoComplete="current-password" />
        </FormField>
        <FormField etiqueta="Contraseña nueva" error={errores.nueva} ayuda="Al menos 8 caracteres, con letras y números.">
          <input type="password" value={nueva} onChange={(e) => setNueva(e.target.value)} autoComplete="new-password" maxLength={72} />
        </FormField>
        <button className="btn btn-secundario" style={{ alignSelf: "flex-start" }} disabled={!actual || !nueva || guardando}>
          {guardando ? "Guardando…" : "Cambiar contraseña"}
        </button>
      </form>
    </section>
  );
}

function Direcciones() {
  const { pedirConSesion } = useAuth();
  const toast = useToast();
  const [lista, setLista] = useState<Direccion[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editando, setEditando] = useState<Direccion | "nueva" | null>(null);
  const [form, setForm] = useState(DIR_VACIA);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [borrar, setBorrar] = useState<Direccion | null>(null);
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(() => {
    setError(null);
    pedirConSesion<Direccion[]>("/api/usuarios/me/direcciones")
      .then(setLista)
      .catch((e: Error) => setError(e.message));
  }, [pedirConSesion]);
  useEffect(cargar, [cargar]);

  function abrir(d: Direccion | "nueva") {
    setErrores({});
    setForm(d === "nueva" ? DIR_VACIA : { calle: d.calle, ciudad: d.ciudad, referencia: d.referencia ?? "" });
    setEditando(d);
  }

  async function guardar(e: React.FormEvent) {
    e.preventDefault();
    if (!editando) return;
    setGuardando(true);
    try {
      if (editando === "nueva") {
        await pedirConSesion("/api/usuarios/me/direcciones", { metodo: "POST", cuerpo: form });
        toast("Dirección agregada");
      } else {
        await pedirConSesion(`/api/usuarios/me/direcciones/${editando.id}`, { metodo: "PUT", cuerpo: form });
        toast("Dirección actualizada");
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
    <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-dir">
      <div className="fila" style={{ justifyContent: "space-between" }}>
        <h2 id="t-dir" style={{ fontSize: "var(--t-lg)" }}>Direcciones de entrega</h2>
        <button className="btn btn-secundario btn-sm" onClick={() => abrir("nueva")} disabled={!lista}>
          <Plus size={15} /> Agregar
        </button>
      </div>
      {error ? (
        <ErrorState mensaje={error} onReintentar={cargar} />
      ) : !lista ? (
        <LoadingSkeleton alto={64} />
      ) : lista.length === 0 ? (
        <EmptyState icono={<MapPin size={26} />} titulo="Sin direcciones" mensaje="Agrega una dirección para recibir tus pedidos." />
      ) : (
        <div className="lista-direcciones">
          {lista.map((d) => (
            <div className="direccion" key={d.id}>
              <div>
                <strong>{d.calle}</strong>
                <div className="texto-2">
                  {d.ciudad}
                  {d.referencia ? ` · ${d.referencia}` : ""}
                </div>
              </div>
              <div className="fila" style={{ gap: 2 }}>
                <button className="btn btn-fantasma btn-icono" onClick={() => abrir(d)} aria-label={`Editar ${d.calle}`}>
                  <Pencil size={16} />
                </button>
                <button className="btn btn-fantasma btn-icono" onClick={() => setBorrar(d)} aria-label={`Eliminar ${d.calle}`}>
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        abierto={editando !== null}
        titulo={editando === "nueva" ? "Nueva dirección" : "Editar dirección"}
        onCerrar={() => setEditando(null)}
      >
        <form className="pila" onSubmit={guardar} noValidate style={{ color: "var(--c-texto)" }}>
          <FormField etiqueta="Calle y número" error={errores.calle}>
            <input value={form.calle} onChange={(e) => setForm({ ...form, calle: e.target.value })} maxLength={120} autoComplete="street-address" />
          </FormField>
          <FormField etiqueta="Ciudad" error={errores.ciudad}>
            <input value={form.ciudad} onChange={(e) => setForm({ ...form, ciudad: e.target.value })} maxLength={50} autoComplete="address-level2" />
          </FormField>
          <FormField etiqueta="Referencia (opcional)" error={errores.referencia} ayuda="Por ejemplo: portón negro, frente a la plaza.">
            <input value={form.referencia} onChange={(e) => setForm({ ...form, referencia: e.target.value })} maxLength={150} />
          </FormField>
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
        titulo="¿Eliminar dirección?"
        mensaje={borrar ? `${borrar.calle}, ${borrar.ciudad}` : ""}
        textoConfirmar="Eliminar"
        peligro
        onCancelar={() => setBorrar(null)}
        onConfirmar={async () => {
          const d = borrar;
          setBorrar(null);
          if (!d) return;
          try {
            await pedirConSesion(`/api/usuarios/me/direcciones/${d.id}`, { metodo: "DELETE" });
            toast("Dirección eliminada", "info");
            cargar();
          } catch (err) {
            toast(err instanceof Error ? err.message : "No se pudo eliminar", "error");
          }
        }}
      />
    </section>
  );
}

/** Configuraciones del armador guardadas por el usuario (tabla configuracion_pc). */
function MisConfiguraciones() {
  const { pedirConSesion } = useAuth();
  const toast = useToast();
  const [lista, setLista] = useState<ResumenConfiguracion[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [borrar, setBorrar] = useState<ResumenConfiguracion | null>(null);

  const cargar = useCallback(() => {
    setError(null);
    pedirConSesion<ResumenConfiguracion[]>("/api/configuraciones")
      .then(setLista)
      .catch((e: Error) => setError(e.message));
  }, [pedirConSesion]);
  useEffect(cargar, [cargar]);

  return (
    <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-config">
      <div className="fila" style={{ justifyContent: "space-between" }}>
        <h2 id="t-config" style={{ fontSize: "var(--t-lg)" }}>Mis configuraciones</h2>
        <Link href="/armador" className="btn btn-secundario btn-sm">
          <Plus size={15} /> Armar una PC
        </Link>
      </div>
      {error ? (
        <ErrorState mensaje={error} onReintentar={cargar} />
      ) : !lista ? (
        <LoadingSkeleton alto={64} />
      ) : lista.length === 0 ? (
        <EmptyState icono={<Cpu size={26} />} titulo="Sin configuraciones" mensaje="Guarda un armado desde el configurador para retomarlo después." />
      ) : (
        <div className="lista-direcciones">
          {lista.map((c) => (
            <div className="configuracion" key={c.id}>
              <div>
                <strong>{c.nombre}</strong>
                <div className="texto-2">
                  {c.componentes} pieza{c.componentes === 1 ? "" : "s"} · {bs(c.precioTotal)}
                  {c.uso ? ` · ${USOS.find((u) => u.id === c.uso)?.etiqueta}` : ""}
                  {" · "}
                  {fechaUtc(c.fechaCreacion).toLocaleDateString("es-BO")}
                </div>
              </div>
              <div className="fila" style={{ gap: 6 }}>
                <CompatibilityBadge severidad={c.estado} />
                <Link href={`/armador?config=${c.id}`} className="btn btn-secundario btn-sm">
                  Abrir
                </Link>
                <button className="btn btn-fantasma btn-icono" onClick={() => setBorrar(c)} aria-label={`Eliminar ${c.nombre}`}>
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
      <ConfirmDialog
        abierto={borrar !== null}
        titulo="¿Eliminar configuración?"
        mensaje={borrar ? `«${borrar.nombre}» se borrará de tu cuenta. Las piezas no se quitan del carrito.` : ""}
        textoConfirmar="Eliminar"
        peligro
        onCancelar={() => setBorrar(null)}
        onConfirmar={async () => {
          const c = borrar;
          setBorrar(null);
          if (!c) return;
          try {
            await pedirConSesion(`/api/configuraciones/${c.id}`, { metodo: "DELETE" });
            toast("Configuración eliminada", "info");
            cargar();
          } catch (err) {
            toast(err instanceof Error ? err.message : "No se pudo eliminar", "error");
          }
        }}
      />
    </section>
  );
}

interface Aviso {
  id: number;
  tipo: string;
  mensaje: string;
  fecha: string;
  estado: "PENDIENTE" | "ENVIADO" | "FALLIDO";
}

/** Avisos que también llegaron por correo: pagos y cambios de estado de los pedidos (RF-20, HU-18). */
function MisAvisos() {
  const { pedirConSesion } = useAuth();
  const [avisos, setAvisos] = useState<Aviso[] | null>(null);
  useEffect(() => {
    pedirConSesion<Aviso[]>("/api/notificaciones").then(setAvisos).catch(() => setAvisos([]));
  }, [pedirConSesion]);
  return (
    <section className="tarjeta tarjeta-cuerpo pila" aria-labelledby="t-avisos">
      <h2 id="t-avisos" style={{ fontSize: "var(--t-lg)" }}>Avisos</h2>
      {avisos === null ? (
        <p className="texto-2">Cargando…</p>
      ) : avisos.length === 0 ? (
        <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>Aquí verás los avisos de tus pagos y pedidos. También te llegan por correo.</p>
      ) : (
        <ul className="lista-io">
          {avisos.map((a) => (
            <li key={a.id}>
              {a.mensaje}{" "}
              <span className="texto-2" style={{ fontSize: "var(--t-xs)" }}>
                · {fechaUtc(a.fecha).toLocaleString("es-BO", { dateStyle: "short", timeStyle: "short" })}
                {a.estado === "PENDIENTE" ? " · correo en cola" : a.estado === "FALLIDO" ? " · no se pudo enviar el correo" : ""}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
