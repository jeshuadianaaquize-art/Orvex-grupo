"use client";

import { ShieldAlert } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth, type Rol } from "@/lib/auth";
import { EmptyState, LoadingSkeleton } from "./Estados";

/** Protege una pantalla en el navegador: sin sesión lleva a "Ingresar" y vuelve después. */
export function RequiereSesion({ roles, children }: { roles?: Rol[]; children: React.ReactNode }) {
  const { usuario, listo } = useAuth();
  const router = useRouter();
  const ruta = usePathname();

  useEffect(() => {
    if (listo && !usuario) router.replace(`/ingresar?siguiente=${encodeURIComponent(ruta)}`);
  }, [listo, usuario, ruta, router]);

  if (!listo || !usuario) {
    return (
      <div className="contenedor pila" style={{ paddingBlock: "var(--s-12)" }} aria-busy="true">
        <LoadingSkeleton alto={28} ancho="40%" />
        <LoadingSkeleton alto={120} />
      </div>
    );
  }
  if (roles && !roles.includes(usuario.rol)) {
    return (
      <div className="contenedor" style={{ paddingBlock: "var(--s-12)" }}>
        <EmptyState
          icono={<ShieldAlert size={26} />}
          titulo="No tienes acceso a esta sección"
          mensaje="Tu cuenta no tiene el rol necesario. Si crees que es un error, comunícate con el administrador."
          accion={
            <Link href="/" className="btn btn-secundario btn-sm">
              Volver al inicio
            </Link>
          }
        />
      </div>
    );
  }
  return <>{children}</>;
}
