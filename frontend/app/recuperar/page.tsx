"use client";

import { Loader2, MailCheck, Send } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { AuthLayout } from "@/components/AuthLayout";
import { FormField } from "@/components/FormField";
import { ErrorApi, pedir } from "@/lib/api";

/** HU-03: pedir el enlace de recuperación. El mensaje es el mismo exista o no la cuenta. */
export default function Recuperar() {
  const [correo, setCorreo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function enviar(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      const r = await pedir<{ mensaje: string }>("/api/auth/recuperar", { metodo: "POST", cuerpo: { correo: correo.trim() } });
      setMensaje(r.mensaje);
    } catch (err) {
      setError(err instanceof ErrorApi ? (err.campos.correo ?? err.message) : "No se pudo enviar la solicitud.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <AuthLayout titulo="Recuperar contraseña" subtitulo="Te enviaremos un enlace para elegir una contraseña nueva.">
      {mensaje ? (
        <div className="pila">
          <div className="alerta alerta-exito" role="status">
            <MailCheck aria-hidden="true" />
            <div>
              {mensaje} El enlace vence en 30 minutos. Revisa también la carpeta de spam.
            </div>
          </div>
          <Link href="/ingresar" className="btn btn-secundario btn-bloque">
            Volver a ingresar
          </Link>
        </div>
      ) : (
        <form onSubmit={enviar} noValidate>
          <FormField etiqueta="Correo de tu cuenta" error={error ?? undefined}>
            <input type="email" autoComplete="email" value={correo} onChange={(e) => setCorreo(e.target.value)} />
          </FormField>
          <button className="btn btn-primario btn-lg btn-bloque" disabled={enviando || !correo.trim()}>
            {enviando ? <Loader2 size={18} className="girar" /> : <Send size={18} />}
            {enviando ? "Enviando…" : "Enviar enlace"}
          </button>
          <p className="pie-auth">
            <Link href="/ingresar">Volver a ingresar</Link>
          </p>
        </form>
      )}
    </AuthLayout>
  );
}
