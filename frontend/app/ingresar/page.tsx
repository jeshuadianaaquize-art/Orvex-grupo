"use client";

import { Loader2, LogIn } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { AuthLayout } from "@/components/AuthLayout";
import { FormField } from "@/components/FormField";
import { useToast } from "@/components/Toast";
import { ErrorApi } from "@/lib/api";
import { destinoSeguro, inicioDeRol, useAuth } from "@/lib/auth";

export default function IngresarPagina() {
  return (
    <Suspense fallback={null}>
      <Ingresar />
    </Suspense>
  );
}

function Ingresar() {
  const { ingresar, usuario, listo } = useAuth();
  const router = useRouter();
  const params = useSearchParams();
  const toast = useToast();
  const [correo, setCorreo] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [ver, setVer] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const siguiente = destinoSeguro(params.get("siguiente"));

  useEffect(() => {
    if (listo && usuario && !enviando) router.replace(siguiente ?? inicioDeRol(usuario.rol));
  }, [listo, usuario, enviando, siguiente, router]);

  async function enviar(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      const u = await ingresar(correo, contrasena);
      toast(`Hola, ${u.nombre.split(" ")[0]}`);
      router.replace(siguiente ?? inicioDeRol(u.rol));
    } catch (err) {
      setError(err instanceof ErrorApi ? err.message : "No se pudo iniciar sesión.");
      setEnviando(false);
    }
  }

  return (
    <AuthLayout titulo="Ingresar" subtitulo="Usa el correo y la contraseña de tu cuenta.">
      <form onSubmit={enviar} noValidate>
        {error && (
          <div className="alerta alerta-error" role="alert">
            {error}
          </div>
        )}
        <FormField etiqueta="Correo electrónico">
          <input type="email" autoComplete="email" required value={correo} onChange={(e) => setCorreo(e.target.value)} />
        </FormField>
        <FormField etiqueta="Contraseña">
          <input
            type={ver ? "text" : "password"}
            autoComplete="current-password"
            required
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
          />
        </FormField>
        <div className="fila" style={{ justifyContent: "space-between" }}>
          <label className="casilla">
            <input type="checkbox" checked={ver} onChange={(e) => setVer(e.target.checked)} /> Mostrar contraseña
          </label>
          <Link href="/recuperar" className="enlace" style={{ fontSize: "var(--t-sm)" }}>
            ¿La olvidaste?
          </Link>
        </div>
        <button className="btn btn-primario btn-lg btn-bloque" disabled={enviando || !correo || !contrasena}>
          {enviando ? <Loader2 size={18} className="girar" /> : <LogIn size={18} />}
          {enviando ? "Ingresando…" : "Ingresar"}
        </button>
      </form>
      <p className="pie-auth">
        ¿No tienes cuenta? <Link href={siguiente ? `/registro?siguiente=${encodeURIComponent(siguiente)}` : "/registro"}>Regístrate</Link>
      </p>
      {process.env.NEXT_PUBLIC_MOSTRAR_DEMO === "true" && (
        <div className="demo">
          <strong>Cuentas de demostración</strong> (contraseña <code>Orvex2026!</code>):
          <ul>
            <li><code>administrador@orvex.demo</code></li>
            <li><code>vendedor@orvex.demo</code> · <code>almacenero@orvex.demo</code></li>
            <li><code>cliente@orvex.demo</code></li>
          </ul>
        </div>
      )}
    </AuthLayout>
  );
}
