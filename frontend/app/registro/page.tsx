"use client";

import { Loader2, UserPlus } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { AuthLayout } from "@/components/AuthLayout";
import { FormField } from "@/components/FormField";
import { useToast } from "@/components/Toast";
import { ErrorApi } from "@/lib/api";
import { destinoSeguro, useAuth } from "@/lib/auth";

export default function RegistroPagina() {
  return (
    <Suspense fallback={null}>
      <Registro />
    </Suspense>
  );
}

const VACIO = { nombre: "", correo: "", telefono: "", contrasena: "" };

/** Validación previa en el navegador; el servidor vuelve a validar todo (HU-01). */
function validar(d: typeof VACIO): Record<string, string> {
  const e: Record<string, string> = {};
  if (!d.nombre.trim()) e.nombre = "escribe tu nombre";
  if (!/^\S+@\S+\.\S+$/.test(d.correo.trim())) e.correo = "no es un correo válido";
  if (d.telefono && !/^\+?[0-9 ]{7,20}$/.test(d.telefono)) e.telefono = "solo números, entre 7 y 20 dígitos";
  if (d.contrasena.length < 8 || !/[A-Za-zÁÉÍÓÚáéíóúÑñ]/.test(d.contrasena) || !/\d/.test(d.contrasena))
    e.contrasena = "debe tener al menos 8 caracteres, con letras y números";
  return e;
}

function Registro() {
  const { registrar } = useAuth();
  const router = useRouter();
  const params = useSearchParams();
  const toast = useToast();
  const [datos, setDatos] = useState(VACIO);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [general, setGeneral] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const set = (k: keyof typeof VACIO) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setDatos((d) => ({ ...d, [k]: e.target.value }));
    setErrores((x) => ({ ...x, [k]: "" }));
  };

  async function enviar(e: React.FormEvent) {
    e.preventDefault();
    setGeneral(null);
    const locales = validar(datos);
    setErrores(locales);
    if (Object.keys(locales).length) return;
    setEnviando(true);
    try {
      await registrar({ ...datos, nombre: datos.nombre.trim(), correo: datos.correo.trim() });
      toast("Tu cuenta está lista. Te enviamos un correo de bienvenida.");
      router.replace(destinoSeguro(params.get("siguiente")) ?? "/cuenta");
    } catch (err) {
      if (err instanceof ErrorApi && Object.keys(err.campos).length) setErrores(err.campos);
      else setGeneral(err instanceof Error ? err.message : "No se pudo crear la cuenta.");
      setEnviando(false);
    }
  }

  return (
    <AuthLayout titulo="Crear cuenta" subtitulo="Para comprar y seguir tus pedidos.">
      <form onSubmit={enviar} noValidate>
        {general && (
          <div className="alerta alerta-error" role="alert">
            {general}
          </div>
        )}
        <FormField etiqueta="Nombre completo" error={errores.nombre}>
          <input autoComplete="name" value={datos.nombre} onChange={set("nombre")} maxLength={80} />
        </FormField>
        <FormField etiqueta="Correo electrónico" error={errores.correo}>
          <input type="email" autoComplete="email" value={datos.correo} onChange={set("correo")} maxLength={100} />
        </FormField>
        <FormField etiqueta="Teléfono (opcional)" error={errores.telefono} ayuda="Para coordinar la entrega.">
          <input type="tel" autoComplete="tel" value={datos.telefono} onChange={set("telefono")} maxLength={20} />
        </FormField>
        <FormField etiqueta="Contraseña" error={errores.contrasena} ayuda="Al menos 8 caracteres, con letras y números.">
          <input type="password" autoComplete="new-password" value={datos.contrasena} onChange={set("contrasena")} maxLength={72} />
        </FormField>
        <button className="btn btn-primario btn-lg btn-bloque" disabled={enviando}>
          {enviando ? <Loader2 size={18} className="girar" /> : <UserPlus size={18} />}
          {enviando ? "Creando cuenta…" : "Crear cuenta"}
        </button>
      </form>
      <p className="pie-auth">
        ¿Ya tienes cuenta? <Link href="/ingresar">Ingresa</Link>
      </p>
    </AuthLayout>
  );
}
