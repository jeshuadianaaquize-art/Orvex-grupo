"use client";

import { LogIn, Menu, ShoppingCart, X } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/lib/auth";
import { useCart } from "@/lib/cart";
import { LogoMarca } from "./iconos";
import { SearchBar } from "./SearchBar";

/** Solo enlaces que ya llevan a una pantalla real */
const ENLACES = [
  { href: "/", texto: "Inicio" },
  { href: "/tienda", texto: "Componentes" },
  { href: "/armador", texto: "Configurador PC" },
];

export function Navbar() {
  const ruta = usePathname();
  const { lineas } = useCart();
  const { usuario, listo } = useAuth();
  const enlaces =
    usuario?.rol === "ADMINISTRADOR"
      ? [...ENLACES, { href: "/admin", texto: "Panel" }, { href: "/inventario", texto: "Inventario" }, { href: "/pos", texto: "Caja" }]
      : usuario?.rol === "ALMACENERO"
        ? [...ENLACES, { href: "/inventario", texto: "Inventario" }]
        : usuario?.rol === "VENDEDOR"
          ? [...ENLACES, { href: "/pos", texto: "Caja" }]
          : ENLACES;
  const [abierto, setAbierto] = useState(false);
  const n = lineas.reduce((s, l) => s + l.cantidad, 0);

  // El contador "salta" cuando se agrega algo al carrito.
  const anterior = useRef(n);
  const [salto, setSalto] = useState(false);
  useEffect(() => {
    if (n > anterior.current) {
      setSalto(true);
      const t = setTimeout(() => setSalto(false), 400);
      anterior.current = n;
      return () => clearTimeout(t);
    }
    anterior.current = n;
  }, [n]);

  useEffect(() => setAbierto(false), [ruta]);

  const actual = (href: string) => (href === "/" ? ruta === "/" : ruta.startsWith(href));

  return (
    <header className="header">
      <div className="contenedor header-fila">
        <button
          className="btn btn-fantasma btn-icono solo-movil"
          onClick={() => setAbierto((a) => !a)}
          aria-expanded={abierto}
          aria-controls="menu-movil"
          aria-label={abierto ? "Cerrar menú" : "Abrir menú"}
        >
          {abierto ? <X size={20} /> : <Menu size={20} />}
        </button>
        <Link href="/" className="logo" aria-label="Orvex, ir al inicio">
          <LogoMarca className="logo-marca" />
          Orvex
        </Link>
        <nav className="nav-principal" aria-label="Principal">
          {enlaces.map((e) => (
            <Link key={e.href} href={e.href} aria-current={actual(e.href) ? "page" : undefined}>
              {e.texto}
            </Link>
          ))}
        </nav>
        <SearchBar key={ruta} />
        <div className="header-acciones">
          {listo &&
            (usuario ? (
              <Link href="/cuenta" className="boton-cuenta" aria-label={`Mi cuenta (${usuario.nombre})`}>
                <span className="avatar" aria-hidden="true">
                  {usuario.nombre.trim().charAt(0).toUpperCase()}
                </span>
                <span className="nombre-cuenta">{usuario.nombre.split(" ")[0]}</span>
              </Link>
            ) : (
              <Link href="/ingresar" className="btn btn-secundario btn-sm" style={{ height: 38 }}>
                <LogIn size={16} /> Ingresar
              </Link>
            ))}
          <Link href="/carrito" className="btn btn-secundario btn-icono boton-carrito" aria-label={`Carrito, ${n} productos`}>
            <ShoppingCart size={18} />
            {n > 0 && <span className={`contador ${salto ? "salto" : ""}`}>{n > 99 ? "99+" : n}</span>}
          </Link>
        </div>
      </div>
      {abierto && (
        <div id="menu-movil" className="menu-movil solo-movil">
          <div className="contenedor">
            <SearchBar onBuscar={() => setAbierto(false)} />
            <nav aria-label="Principal (móvil)">
              {enlaces.map((e) => (
                <Link key={e.href} href={e.href} aria-current={actual(e.href) ? "page" : undefined}>
                  {e.texto}
                </Link>
              ))}
              <Link href="/carrito">Carrito{n > 0 ? ` (${n})` : ""}</Link>
              {usuario && <Link href="/pedidos">Mis pedidos</Link>}
              <Link href={usuario ? "/cuenta" : "/ingresar"}>{usuario ? "Mi cuenta" : "Ingresar"}</Link>
            </nav>
          </div>
        </div>
      )}
    </header>
  );
}
