"use client";

import { KeyRound, Loader2 } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { AuthLayout } from "@/components/AuthLayout";
import { EmptyState } from "@/components/Estados";
import { FormField } from "@/components/FormField";
import { ErrorApi, pedir } from "@/lib/api";

export default function RestablecerPagina() {
  return (
    <Suspense fallback={null}>
      <Restablecer />
    </Suspense>
  );
}

/** HU-03: elegir la contraseña nueva desde el enlace del correo. */
function Restablecer() {
  const token = useSearchParams().get("token") ?? "";
  const [contrasena, setContrasena] = useState("");
  const [repetir, setRepetir] = useState("");
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [general, setGeneral] = useState<string | null>(null);
  const [listo, setListo] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e: React.FormEvent) {
    e.preventDefault();
    setGeneral(null);
    if (contrasena !== repetir) {
      setErrores({ repetir: "las contraseñas no coinciden" });
      return;
    }
    setErrores({});
    setEnviando(true);
    try {
      const r = await pedir<{ mensaje: string }>("/api/auth/restablecer", { metodo: "POST", cuerpo: { token, contrasena } });
      setListo(r.mensaje);
    } catch (err) {
      if (err instanceof ErrorApi && err.campos.contrasena) setErrores({ contrasena: err.campos.contrasena });
      else setGeneral(err instanceof Error ? err.message : "No se pudo cambiar la contraseña.");
    } finally {
      setEnviando(false);
    }
  }

  if (!token) {
    return (
      <AuthLayout titulo="Enlace incompleto">
        <EmptyState
          titulo="Falta el código del enlace"
          mensaje="Abre el enlace completo que te enviamos por correo, o solicita uno nuevo."
          accion={<Link href="/recuperar" className="btn btn-primario btn-sm">Solicitar enlace</Link>}
        />
      </AuthLayout>
    );
  }

  return (
    <AuthLayout titulo="Nueva contraseña" subtitulo="Elige una contraseña que no uses en otros sitios.">
      {listo ? (
        <div className="pila">
          <div className="alerta alerta-exito" role="status">{listo}</div>
          <Link href="/ingresar" className="btn btn-primario btn-bloque">Ingresar</Link>
        </div>
      ) : (
        <form onSubmit={enviar} noValidate>
          {general && (
            <div className="alerta alerta-error" role="alert">
              <div>
                {general} <Link href="/recuperar" className="enlace">Solicitar un enlace nuevo</Link>
              </div>
            </div>
          )}
          <FormField etiqueta="Contraseña nueva" error={errores.contrasena} ayuda="Al menos 8 caracteres, con letras y números.">
            <input type="password" autoComplete="new-password" value={contrasena} onChange={(e) => setContrasena(e.target.value)} maxLength={72} />
          </FormField>
          <FormField etiqueta="Repite la contraseña" error={errores.repetir}>
            <input type="password" autoComplete="new-password" value={repetir} onChange={(e) => setRepetir(e.target.value)} maxLength={72} />
          </FormField>
          <button className="btn btn-primario btn-lg btn-bloque" disabled={enviando || !contrasena || !repetir}>
            {enviando ? <Loader2 size={18} className="girar" /> : <KeyRound size={18} />}
            {enviando ? "Guardando…" : "Guardar contraseña"}
          </button>
        </form>
      )}
    </AuthLayout>
  );
}
