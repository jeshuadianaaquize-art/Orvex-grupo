"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { ErrorApi, pedir } from "./api";

export type Rol = "CLIENTE" | "VENDEDOR" | "ALMACENERO" | "ADMINISTRADOR";

export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  telefono: string | null;
  rol: Rol;
  estado: "ACTIVO" | "BLOQUEADO";
  fechaRegistro: string;
}

interface Sesion {
  token: string;
  expira: string;
  usuario: Usuario;
}

export const ETIQUETA_ROL: Record<Rol, string> = {
  CLIENTE: "Cliente",
  VENDEDOR: "Vendedor",
  ALMACENERO: "Almacenero",
  ADMINISTRADOR: "Administrador",
};

/** Pantalla inicial de cada rol después de iniciar sesión (mapa navegacional del informe). */
export function inicioDeRol(rol: Rol): string {
  if (rol === "ADMINISTRADOR") return "/admin";
  if (rol === "VENDEDOR") return "/pos";
  if (rol === "ALMACENERO") return "/inventario";
  return "/cuenta";
}

/** Solo rutas internas: evita redirecciones abiertas a otros sitios (OWASP). */
export function destinoSeguro(siguiente: string | null): string | null {
  return siguiente && siguiente.startsWith("/") && !siguiente.startsWith("//") && !siguiente.startsWith("/\\")
    ? siguiente
    : null;
}

interface Ctx {
  usuario: Usuario | null;
  token: string | null;
  /** false hasta leer la sesión guardada: evita redirigir a "ingresar" antes de saber si hay sesión. */
  listo: boolean;
  ingresar: (correo: string, contrasena: string) => Promise<Usuario>;
  registrar: (datos: { nombre: string; correo: string; contrasena: string; telefono: string }) => Promise<Usuario>;
  salir: () => Promise<void>;
  actualizarUsuario: (u: Usuario) => void;
  /** Petición con la sesión actual. Si el servidor responde 401, la sesión se cierra en el navegador. */
  pedirConSesion: <T>(ruta: string, opciones?: { metodo?: string; cuerpo?: unknown }) => Promise<T>;
}

const AuthCtx = createContext<Ctx | null>(null);
const CLAVE = "orvex-sesion";

/* El token se guarda en localStorage para que la sesión sobreviva a una recarga. */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(null);
  const [listo, setListo] = useState(false);

  const guardar = useCallback((s: Sesion | null) => {
    setSesion(s);
    try {
      if (s) localStorage.setItem(CLAVE, JSON.stringify(s));
      else localStorage.removeItem(CLAVE);
    } catch {
      /* sin almacenamiento: la sesión dura lo que la pestaña */
    }
  }, []);

  useEffect(() => {
    try {
      const s = JSON.parse(localStorage.getItem(CLAVE) ?? "null") as Sesion | null;
      if (s?.token && new Date(s.expira).getTime() > Date.now()) setSesion(s);
      else localStorage.removeItem(CLAVE);
    } catch {
      /* sesión guardada ilegible */
    }
    setListo(true);
  }, []);

  // Cierra la sesión en el navegador cuando el token vence.
  useEffect(() => {
    if (!sesion) return;
    const ms = new Date(sesion.expira).getTime() - Date.now();
    const t = setTimeout(() => guardar(null), Math.max(0, Math.min(ms, 2_147_000_000)));
    return () => clearTimeout(t);
  }, [sesion, guardar]);

  const ingresar = useCallback(
    async (correo: string, contrasena: string) => {
      const s = await pedir<Sesion>("/api/auth/login", { metodo: "POST", cuerpo: { correo, contrasena } });
      guardar(s);
      return s.usuario;
    },
    [guardar],
  );

  const registrar = useCallback(
    async (datos: { nombre: string; correo: string; contrasena: string; telefono: string }) => {
      const s = await pedir<Sesion>("/api/auth/registro", { metodo: "POST", cuerpo: datos });
      guardar(s);
      return s.usuario;
    },
    [guardar],
  );

  const salir = useCallback(async () => {
    const token = sesion?.token;
    guardar(null);
    if (token) {
      // El gateway invalida el token. Si falla (sin red), la sesión ya se cerró en este navegador.
      await pedir("/api/auth/logout", { metodo: "POST", token }).catch(() => undefined);
    }
  }, [sesion, guardar]);

  const pedirConSesion = useCallback(
    async <T,>(ruta: string, opciones: { metodo?: string; cuerpo?: unknown } = {}) => {
      try {
        return await pedir<T>(ruta, { ...opciones, token: sesion?.token });
      } catch (e) {
        if (e instanceof ErrorApi && e.estado === 401) guardar(null);
        throw e;
      }
    },
    [sesion, guardar],
  );

  const actualizarUsuario = useCallback(
    (u: Usuario) => setSesion((s) => {
      if (!s) return s;
      const nueva = { ...s, usuario: u };
      try {
        localStorage.setItem(CLAVE, JSON.stringify(nueva));
      } catch {
        /* ignorar */
      }
      return nueva;
    }),
    [],
  );

  const valor = useMemo(
    () => ({
      usuario: sesion?.usuario ?? null,
      token: sesion?.token ?? null,
      listo,
      ingresar,
      registrar,
      salir,
      actualizarUsuario,
      pedirConSesion,
    }),
    [sesion, listo, ingresar, registrar, salir, actualizarUsuario, pedirConSesion],
  );

  return <AuthCtx.Provider value={valor}>{children}</AuthCtx.Provider>;
}

export function useAuth(): Ctx {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error("useAuth debe usarse dentro de AuthProvider");
  return ctx;
}
