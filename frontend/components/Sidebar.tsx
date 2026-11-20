"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export interface EnlaceSidebar {
  href: string;
  texto: string;
  icono: React.ReactNode;
}

export interface GrupoSidebar {
  titulo: string;
  enlaces: EnlaceSidebar[];
}

/** Navegación lateral de los paneles (cliente, admin) */
/** Activo si coincide exacto, o si es el prefijo más largo que coincide (p. ej. /admin/productos/12). */
function activo(ruta: string, href: string, enlaces: EnlaceSidebar[]) {
  if (ruta === href) return true;
  if (!ruta.startsWith(`${href}/`)) return false;
  return !enlaces.some((o) => o.href !== href && o.href.startsWith(href) && (ruta === o.href || ruta.startsWith(`${o.href}/`)));
}

export function Sidebar({ grupos, etiqueta = "Navegación del panel" }: { grupos: GrupoSidebar[]; etiqueta?: string }) {
  const ruta = usePathname();
  // Se compara con los enlaces de todos los grupos: "/admin" no queda activo dentro de "/admin/productos".
  const todos = grupos.flatMap((g) => g.enlaces);
  return (
    <nav className="sidebar" aria-label={etiqueta}>
      {grupos.map((g) => (
        <div key={g.titulo}>
          <div className="sidebar-grupo">{g.titulo}</div>
          {g.enlaces.map((e) => (
            <Link key={e.href} href={e.href} aria-current={activo(ruta, e.href, todos) ? "page" : undefined}>
              {e.icono}
              {e.texto}
            </Link>
          ))}
        </div>
      ))}
    </nav>
  );
}
