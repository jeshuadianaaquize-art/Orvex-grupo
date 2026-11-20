import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import "./globals.css";
import "./neon.css";
import "./paneles.css";
import "./carbon.css";
import { Footer } from "@/components/Footer";
import { Navbar } from "@/components/Navbar";
import { ToastProvider } from "@/components/Toast";
import { AuthProvider } from "@/lib/auth";
import { CartProvider } from "@/lib/cart";

// Fuentes servidas desde el propio sitio
const body = localFont({
  src: "../node_modules/@fontsource-variable/inter/files/inter-latin-wght-normal.woff2",
  weight: "100 900",
  variable: "--font-body",
  display: "swap",
});
const display = localFont({
  src: [
    { path: "../node_modules/@fontsource/space-grotesk/files/space-grotesk-latin-600-normal.woff2", weight: "600" },
    { path: "../node_modules/@fontsource/space-grotesk/files/space-grotesk-latin-700-normal.woff2", weight: "700" },
  ],
  variable: "--font-display",
  display: "swap",
});
// Tema "Carbón": títulos grandes y delgados (Genos) y acentos técnicos (Orbitron), también autoalojados.
const titulo = localFont({
  src: [
    { path: "../node_modules/@fontsource/genos/files/genos-latin-300-normal.woff2", weight: "300" },
    { path: "../node_modules/@fontsource/genos/files/genos-latin-500-normal.woff2", weight: "500" },
  ],
  variable: "--font-titulo",
  display: "swap",
});
const tech = localFont({
  src: [
    { path: "../node_modules/@fontsource/orbitron/files/orbitron-latin-500-normal.woff2", weight: "500" },
    { path: "../node_modules/@fontsource/orbitron/files/orbitron-latin-700-normal.woff2", weight: "700" },
  ],
  variable: "--font-tech",
  display: "swap",
});
const mono = localFont({
  src: "../node_modules/@fontsource/jetbrains-mono/files/jetbrains-mono-latin-500-normal.woff2",
  weight: "500",
  variable: "--font-mono",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "Orvex · Arma tu PC sin errores", template: "%s · Orvex" },
  description: "Componentes de PC con verificación de compatibilidad y consumo antes de comprar.",
};

export const viewport: Viewport = { themeColor: "#131313" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es" className={`${body.variable} ${display.variable} ${titulo.variable} ${tech.variable} ${mono.variable}`}>
      <body>
        <ToastProvider>
          <AuthProvider>
          <CartProvider>
            <a href="#contenido" className="sr-only">
              Saltar al contenido
            </a>
            <Navbar />
            <main id="contenido">{children}</main>
            <Footer />
          </CartProvider>
          </AuthProvider>
        </ToastProvider>
      </body>
    </html>
  );
}
