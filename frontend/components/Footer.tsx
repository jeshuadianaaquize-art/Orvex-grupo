import Link from "next/link";
import { CATEGORIAS } from "@/lib/api";
import { LogoMarca } from "./iconos";

/** Pie de página. Sin teléfonos ni direcciones inventados: la tienda real todavía no los definió. */
export function Footer() {
  return (
    <footer className="footer">
      <div className="contenedor">
        <div>
          <div className="logo">
            <LogoMarca className="logo-marca" /> Orvex
          </div>
          <p>Componentes de PC con verificación de compatibilidad antes de comprar. Precios en bolivianos.</p>
        </div>
        <div>
          <h3>Componentes</h3>
          <ul>
            {CATEGORIAS.map((c) => (
              <li key={c.id}>
                <Link href={`/tienda?cat=${c.id}`}>{c.etiqueta}</Link>
              </li>
            ))}
          </ul>
        </div>
        <div>
          <h3>Comprar</h3>
          <ul>
            <li>
              <Link href="/armador">Configurador de PC</Link>
            </li>
            <li>
              <Link href="/tienda">Catálogo completo</Link>
            </li>
            <li>
              <Link href="/carrito">Carrito</Link>
            </li>
            <li>
              <Link href="/#como-funciona">Cómo funciona</Link>
            </li>
          </ul>
        </div>
        <div>
          <h3>Proyecto</h3>
          <ul>
            <li>Grupo Los Llajta Bytes</li>
            <li>Universidad Privada Franz Tamayo</li>
            <li>Ingeniería de Sistemas</li>
            <li>
              <Link href="/creditos">Créditos de las fotos</Link>
            </li>
          </ul>
        </div>
      </div>
      <div className="contenedor footer-legal">
        Versión beta · Los precios y datos del catálogo son de ejemplo · Cada venta genera un recibo interno no fiscal.
      </div>
    </footer>
  );
}
