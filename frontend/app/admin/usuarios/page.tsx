"use client";

import { Search } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { StatusBadge } from "@/components/Badges";
import { DataTable } from "@/components/DataTable";
import { ErrorState, LoadingSkeleton } from "@/components/Estados";
import { ConfirmDialog } from "@/components/Modal";
import { Pagination } from "@/components/Pagination";
import { useToast } from "@/components/Toast";
import { ETIQUETA_ROL, useAuth, type Rol, type Usuario } from "@/lib/auth";

interface Pagina {
  elementos: Usuario[];
  pagina: number;
  totalPaginas: number;
  total: number;
}

type Cambio = { usuario: Usuario; rol?: Rol; estado?: "ACTIVO" | "BLOQUEADO" };

const ROLES = Object.keys(ETIQUETA_ROL) as Rol[];

/** HU-19: lista de usuarios, asignación de rol y bloqueo de cuentas, con confirmación. */
export default function AdminUsuarios() {
  const { usuario: yo, pedirConSesion } = useAuth();
  const toast = useToast();
  const [datos, setDatos] = useState<Pagina | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [buscar, setBuscar] = useState("");
  const [buscarAplicado, setBuscarAplicado] = useState("");
  const [rol, setRol] = useState("");
  const [estado, setEstado] = useState("");
  const [pagina, setPagina] = useState(0);
  const [cambio, setCambio] = useState<Cambio | null>(null);

  // La búsqueda por texto espera a que se deje de escribir.
  useEffect(() => {
    const t = setTimeout(() => {
      setBuscarAplicado(buscar.trim());
      setPagina(0);
    }, 300);
    return () => clearTimeout(t);
  }, [buscar]);

  const cargar = useCallback(() => {
    setError(null);
    const q = new URLSearchParams({ pagina: String(pagina) });
    if (buscarAplicado) q.set("buscar", buscarAplicado);
    if (rol) q.set("rol", rol);
    if (estado) q.set("estado", estado);
    pedirConSesion<Pagina>(`/api/usuarios?${q}`)
      .then(setDatos)
      .catch((e: Error) => setError(e.message));
  }, [pedirConSesion, pagina, buscarAplicado, rol, estado]);
  useEffect(cargar, [cargar]);

  async function aplicar(c: Cambio) {
    setCambio(null);
    try {
      await pedirConSesion(`/api/usuarios/${c.usuario.id}`, { metodo: "PATCH", cuerpo: { rol: c.rol, estado: c.estado } });
      toast(c.rol ? `${c.usuario.nombre} ahora es ${ETIQUETA_ROL[c.rol]}` : `Cuenta ${c.estado === "BLOQUEADO" ? "bloqueada" : "reactivada"}`);
      cargar();
    } catch (e) {
      toast(e instanceof Error ? e.message : "No se pudo guardar", "error");
    }
  }

  return (
    <>
      <div className="panel-cabecera">
        <div>
          <span className="eyebrow">Acceso</span>
          <h1>Usuarios y roles</h1>
          <p className="texto-2" style={{ fontSize: "var(--t-sm)" }}>
            {datos ? `${datos.total} usuario${datos.total === 1 ? "" : "s"}` : "Cargando…"}
          </p>
        </div>
      </div>

      <div className="barra-filtros">
        <div className="busqueda buscador" style={{ display: "block", maxWidth: "none", margin: 0 }}>
          <Search aria-hidden="true" />
          <input
            type="search"
            className="entrada"
            style={{ paddingLeft: 38 }}
            placeholder="Buscar por nombre o correo"
            value={buscar}
            onChange={(e) => setBuscar(e.target.value)}
            aria-label="Buscar usuarios"
          />
        </div>
        <select className="entrada" value={rol} onChange={(e) => { setRol(e.target.value); setPagina(0); }} aria-label="Filtrar por rol">
          <option value="">Todos los roles</option>
          {ROLES.map((r) => (
            <option key={r} value={r}>{ETIQUETA_ROL[r]}</option>
          ))}
        </select>
        <select className="entrada" value={estado} onChange={(e) => { setEstado(e.target.value); setPagina(0); }} aria-label="Filtrar por estado">
          <option value="">Todos los estados</option>
          <option value="ACTIVO">Activos</option>
          <option value="BLOQUEADO">Bloqueados</option>
        </select>
      </div>

      {error ? (
        <ErrorState mensaje={error} onReintentar={cargar} />
      ) : !datos ? (
        <LoadingSkeleton alto={240} radio={16} />
      ) : (
        <>
          <DataTable
            titulo="Usuarios"
            vacio="Ningún usuario coincide con la búsqueda."
            filas={datos.elementos}
            claveFila={(u) => u.id}
            columnas={[
              {
                clave: "usuario",
                titulo: "Usuario",
                render: (u) => (
                  <div>
                    <strong>{u.nombre}</strong>
                    {u.id === yo?.id && <span className="texto-2"> (tú)</span>}
                    <div className="texto-2" style={{ fontSize: "var(--t-xs)" }}>{u.correo}</div>
                  </div>
                ),
              },
              {
                clave: "rol",
                titulo: "Rol",
                render: (u) => (
                  <select
                    className="entrada"
                    style={{ height: 34, minWidth: 150 }}
                    value={u.rol}
                    disabled={u.id === yo?.id}
                    onChange={(e) => setCambio({ usuario: u, rol: e.target.value as Rol })}
                    aria-label={`Rol de ${u.nombre}`}
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>{ETIQUETA_ROL[r]}</option>
                    ))}
                  </select>
                ),
              },
              {
                clave: "estado",
                titulo: "Estado",
                render: (u) => (
                  <StatusBadge tono={u.estado === "ACTIVO" ? "exito" : "error"}>
                    {u.estado === "ACTIVO" ? "Activo" : "Bloqueado"}
                  </StatusBadge>
                ),
              },
              {
                clave: "registro",
                titulo: "Registro",
                render: (u) => new Date(u.fechaRegistro).toLocaleDateString("es-BO"),
              },
              {
                clave: "acciones",
                titulo: "Acciones",
                render: (u) =>
                  u.id === yo?.id ? (
                    <span className="texto-2" style={{ fontSize: "var(--t-xs)" }}>—</span>
                  ) : (
                    <button
                      className={`btn btn-sm ${u.estado === "ACTIVO" ? "btn-secundario" : "btn-primario"}`}
                      onClick={() => setCambio({ usuario: u, estado: u.estado === "ACTIVO" ? "BLOQUEADO" : "ACTIVO" })}
                    >
                      {u.estado === "ACTIVO" ? "Bloquear" : "Reactivar"}
                    </button>
                  ),
              },
            ]}
          />
          <Pagination pagina={datos.pagina + 1} totalPaginas={datos.totalPaginas} onCambiar={(p) => setPagina(p - 1)} />
        </>
      )}

      <ConfirmDialog
        abierto={cambio !== null}
        titulo={cambio?.rol ? "¿Cambiar el rol?" : cambio?.estado === "BLOQUEADO" ? "¿Bloquear la cuenta?" : "¿Reactivar la cuenta?"}
        mensaje={
          cambio?.rol
            ? `${cambio.usuario.nombre} pasará de ${ETIQUETA_ROL[cambio.usuario.rol]} a ${ETIQUETA_ROL[cambio.rol]}. Sus permisos cambian en menos de un minuto.`
            : cambio?.estado === "BLOQUEADO"
              ? `${cambio.usuario.nombre} no podrá ingresar y su sesión actual se cerrará en menos de un minuto.`
              : `${cambio?.usuario.nombre} podrá volver a ingresar.`
        }
        textoConfirmar={cambio?.rol ? "Cambiar rol" : cambio?.estado === "BLOQUEADO" ? "Bloquear" : "Reactivar"}
        peligro={cambio?.estado === "BLOQUEADO" || cambio?.rol === "ADMINISTRADOR"}
        onCancelar={() => setCambio(null)}
        onConfirmar={() => cambio && aplicar(cambio)}
      />
    </>
  );
}
