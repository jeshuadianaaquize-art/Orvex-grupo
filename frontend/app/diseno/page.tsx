"use client";

import { Boxes, ClipboardList, Gauge, LayoutDashboard, Package, ShieldCheck, Users } from "lucide-react";
import { useState } from "react";
import { CompatibilityBadge, StatusBadge, StockBadge } from "@/components/Badges";
import { CartItem } from "@/components/CartItem";
import { CategoryCard } from "@/components/CategoryCard";
import { ChartCard } from "@/components/ChartCard";
import { ComponentSelector } from "@/components/ComponentSelector";
import { DataTable } from "@/components/DataTable";
import { EmptyState, ErrorState, LoadingSkeleton, ProductCardSkeleton } from "@/components/Estados";
import { FILTROS_VACIOS, FilterPanel } from "@/components/FilterPanel";
import { FormField } from "@/components/FormField";
import { ConfirmDialog, Modal } from "@/components/Modal";
import { Pagination } from "@/components/Pagination";
import { PcSummary } from "@/components/PcSummary";
import { PriceDisplay } from "@/components/PriceDisplay";
import { ProductCard } from "@/components/ProductCard";
import { Sidebar } from "@/components/Sidebar";
import { useToast } from "@/components/Toast";
import type { Producto, Reporte } from "@/lib/api";

/* Guía viva del sistema de diseño */
const EJ_CPU: Producto = { id: -1, sku: "EJEMPLO-1", nombre: "Procesador de ejemplo 6 núcleos", marca: "Marca", precio: 1100, fotoUrl: null, stock: 12, enStock: true, categoria: "PROCESADOR", specs: { socket: "AM4", tdp_w: "65" } };
const EJ_PLACA: Producto = { id: -2, sku: "EJEMPLO-2", nombre: "Placa madre de ejemplo mATX", marca: "Marca", precio: 780, fotoUrl: null, stock: 2, enStock: true, categoria: "PLACA_MADRE", specs: { socket: "AM4", tipo_ram: "DDR4", factor_forma: "mATX" } };
const EJ_GPU: Producto = { id: -3, sku: "EJEMPLO-3", nombre: "Tarjeta gráfica de ejemplo 8 GB", marca: "Marca", precio: 3300, fotoUrl: null, stock: 0, enStock: false, categoria: "TARJETA_GRAFICA", specs: { largo_mm: "240", tdp_w: "115" } };
const EJ_REPORTE: Reporte = {
  global: "ADVERTENCIA",
  consumoEstimadoW: 280,
  resultados: [
    { reglaId: null, regla: "Socket del procesador", severidad: "COMPATIBLE", mensaje: "Ambos usan el socket AM4." },
    { reglaId: null, regla: "Consumo y fuente de poder", severidad: "ADVERTENCIA", mensaje: "La fuente alcanza pero con poco margen." },
    { reglaId: null, regla: "Tipo de memoria RAM", severidad: "INCOMPATIBLE", mensaje: "La RAM es DDR5 pero la placa admite DDR4." },
  ],
};

const COLORES = [
  ["--noche-950", "#0B1020", "Fondo oscuro"],
  ["--noche-900", "#111827", "Superficie oscura · texto"],
  ["--noche-800", "#182235", "Superficie elevada"],
  ["--gris-50", "#F5F7FB", "Fondo claro"],
  ["--azul-600", "#2563EB", "Primario"],
  ["--azul-500", "#3B82F6", "Interacción"],
  ["--cian-500", "#06B6D4", "Detalles"],
  ["--gris-500", "#64748B", "Texto secundario"],
  ["--verde-600", "#16A34A", "Éxito"],
  ["--ambar-500", "#F59E0B", "Advertencia"],
  ["--rojo-600", "#DC2626", "Error"],
];

function Bloque({ titulo, nombre, children }: { titulo: string; nombre?: string; children: React.ReactNode }) {
  return (
    <section className="pila" style={{ paddingBlock: "var(--s-8)", borderTop: "1px solid var(--c-borde)" }}>
      <div>
        <h2 style={{ fontSize: "var(--t-xl)" }}>{titulo}</h2>
        {nombre && <code className="texto-2" style={{ fontSize: "var(--t-xs)" }}>{nombre}</code>}
      </div>
      {children}
    </section>
  );
}

export default function Diseno() {
  const toast = useToast();
  const [modal, setModal] = useState(false);
  const [confirmar, setConfirmar] = useState(false);
  const [pagina, setPagina] = useState(3);
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const [elegido, setElegido] = useState<number | undefined>(-2);
  const [cantidad, setCantidad] = useState(1);
  const [correo, setCorreo] = useState("correo-invalido");

  return (
    <div className="contenedor" style={{ paddingBlock: "var(--s-8)" }}>
      <span className="eyebrow">Guía interna</span>
      <h1 style={{ fontSize: "var(--t-3xl)", margin: "6px 0" }}>Sistema de diseño Orvex</h1>
      <p className="texto-2" style={{ maxWidth: "46rem" }}>
        Tokens y componentes reutilizables. Todos los datos de esta página son de ejemplo. La paleta es una propuesta visual:
        se cambia editando los tokens de <code>app/globals.css</code>.
      </p>

      <Bloque titulo="Colores">
        <div className="rejilla" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(150px, 1fr))" }}>
          {COLORES.map(([token, hex, uso]) => (
            <div key={token} className="tarjeta" style={{ overflow: "hidden" }}>
              <div style={{ height: 64, background: `var(${token})` }} />
              <div style={{ padding: "var(--s-3)", fontSize: "var(--t-xs)" }}>
                <strong>{uso}</strong>
                <div className="texto-2">
                  {hex} · <code>{token}</code>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Bloque>

      <Bloque titulo="Tipografía">
        <h1 style={{ fontSize: "var(--t-3xl)" }}>Space Grotesk · Títulos</h1>
        <p>Inter · Texto de lectura, formularios y tablas, en 16 px por defecto.</p>
        <span className="eyebrow">JetBrains Mono · Etiquetas y datos técnicos</span>
        <div className="fila">
          <PriceDisplay monto={7210} tamano="lg" /> <PriceDisplay monto={1100} /> <PriceDisplay monto={380} tamano="sm" />
        </div>
      </Bloque>

      <Bloque titulo="Botones">
        <div className="fila" style={{ flexWrap: "wrap" }}>
          <button className="btn btn-primario btn-lg">Primario grande</button>
          <button className="btn btn-primario">Primario</button>
          <button className="btn btn-secundario">Secundario</button>
          <button className="btn btn-fantasma">Fantasma</button>
          <button className="btn btn-peligro">Peligro</button>
          <button className="btn btn-primario" disabled>
            Deshabilitado
          </button>
          <button className="btn btn-secundario btn-sm">Pequeño</button>
        </div>
      </Bloque>

      <Bloque titulo="Insignias" nombre="StockBadge · CompatibilityBadge · StatusBadge">
        <div className="fila" style={{ flexWrap: "wrap" }}>
          <StockBadge stock={12} enStock /> <StockBadge stock={2} enStock /> <StockBadge stock={0} enStock={false} />
          <StockBadge stock={null} enStock={null} />
          <CompatibilityBadge severidad="COMPATIBLE" /> <CompatibilityBadge severidad="ADVERTENCIA" />
          <CompatibilityBadge severidad="INCOMPATIBLE" />
          <StatusBadge tono="info">Pendiente</StatusBadge> <StatusBadge tono="exito">Pagado</StatusBadge>
          <StatusBadge tono="aviso">Por preparar</StatusBadge> <StatusBadge>Entregado</StatusBadge>
        </div>
      </Bloque>

      <Bloque titulo="Formularios" nombre="FormField">
        <div className="rejilla">
          <FormField etiqueta="Nombre completo" ayuda="Como aparece en tu documento.">
            <input defaultValue="" placeholder="Ej.: Ana Pérez" />
          </FormField>
          <FormField etiqueta="Correo" error={correo.includes("@") ? undefined : "Escribe un correo válido."}>
            <input value={correo} onChange={(e) => setCorreo(e.target.value)} />
          </FormField>
          <FormField etiqueta="Rol">
            <select defaultValue="cliente">
              <option value="cliente">Cliente</option>
              <option value="vendedor">Vendedor</option>
            </select>
          </FormField>
        </div>
      </Bloque>

      <Bloque titulo="Tarjetas" nombre="ProductCard · CategoryCard">
        <div className="rejilla">
          <ProductCard producto={EJ_CPU} onAgregar={() => toast("Ejemplo: agregado al carrito")} />
          <ProductCard producto={EJ_GPU} onAgregar={() => undefined} />
          <ProductCardSkeleton />
          <CategoryCard categoria="REFRIGERACION" nombre="Refrigeración" descripcion="Temperaturas bajo control" cantidad={2} desde={250} />
        </div>
      </Bloque>

      <Bloque titulo="Filtros, carrito y paginación" nombre="FilterPanel · CartItem · Pagination">
        <div className="rejilla" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 300px), 1fr))", alignItems: "start" }}>
          <FilterPanel
            filtros={filtros}
            onCambiar={setFiltros}
            facetas={{ marcas: ["AMD", "Intel", "NVIDIA"], sockets: ["AM4", "AM5", "LGA1700"], tiposRam: ["DDR4", "DDR5"], precioMaximo: 5200 }}
          />
          <div className="pila">
            <div className="tarjeta">
              <CartItem
                linea={{
                  clave: "ej",
                  itemId: null,
                  tipo: "PRODUCTO",
                  productoId: EJ_PLACA.id,
                  configuracionId: null,
                  nombre: EJ_PLACA.nombre,
                  categoria: EJ_PLACA.categoria,
                  marca: EJ_PLACA.marca,
                  fotoUrl: EJ_PLACA.fotoUrl,
                  cantidad,
                  precioUnitario: EJ_PLACA.precio,
                  subtotal: EJ_PLACA.precio * cantidad,
                  stock: EJ_PLACA.stock,
                  enStock: EJ_PLACA.enStock,
                  componentes: [],
                }} onCantidad={(c) => setCantidad(Math.max(1, c))} onQuitar={() => toast("Ejemplo: quitado", "info")} />
            </div>
            <Pagination pagina={pagina} totalPaginas={9} onCambiar={setPagina} />
          </div>
        </div>
      </Bloque>

      <Bloque titulo="Retroalimentación" nombre="Modal · ConfirmDialog · ToastNotification">
        <div className="fila" style={{ flexWrap: "wrap" }}>
          <button className="btn btn-secundario" onClick={() => setModal(true)}>
            Abrir modal
          </button>
          <button className="btn btn-secundario" onClick={() => setConfirmar(true)}>
            Abrir confirmación
          </button>
          <button className="btn btn-secundario" onClick={() => toast("Cambios guardados")}>
            Aviso de éxito
          </button>
          <button className="btn btn-secundario" onClick={() => toast("No se pudo guardar", "error")}>
            Aviso de error
          </button>
        </div>
        <div className="alerta alerta-aviso">
          <ShieldCheck aria-hidden="true" /> Alerta de advertencia con icono y texto: el color nunca es la única señal.
        </div>
        <Modal abierto={modal} titulo="Modal de ejemplo" onCerrar={() => setModal(false)} pie={<button className="btn btn-primario" onClick={() => setModal(false)}>Entendido</button>}>
          Se cierra con Escape, con el botón o haciendo clic fuera.
        </Modal>
        <ConfirmDialog
          abierto={confirmar}
          titulo="¿Desactivar producto?"
          mensaje="Dejará de mostrarse en el catálogo."
          textoConfirmar="Desactivar"
          peligro
          onCancelar={() => setConfirmar(false)}
          onConfirmar={() => {
            setConfirmar(false);
            toast("Ejemplo: confirmado");
          }}
        />
      </Bloque>

      <Bloque titulo="Estados" nombre="LoadingSkeleton · EmptyState · ErrorState">
        <div className="rejilla" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 300px), 1fr))" }}>
          <div className="tarjeta tarjeta-cuerpo pila">
            <LoadingSkeleton alto={20} ancho="60%" />
            <LoadingSkeleton />
            <LoadingSkeleton ancho="80%" />
          </div>
          <EmptyState titulo="Sin pedidos todavía" mensaje="Cuando compres, tus pedidos aparecerán aquí." />
          <ErrorState mensaje="El servicio no respondió a tiempo." onReintentar={() => toast("Ejemplo: reintentando", "info")} />
        </div>
      </Bloque>

      <Bloque titulo="Datos" nombre="DataTable · ChartCard">
        <div className="rejilla" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 380px), 1fr))", alignItems: "start" }}>
          <DataTable
            titulo="Pedidos de ejemplo"
            claveFila={(f) => f.id}
            filas={[
              { id: 101, cliente: "Cliente de ejemplo", estado: "Pagado", total: 7210 },
              { id: 102, cliente: "Cliente de ejemplo", estado: "Pendiente", total: 1880 },
            ]}
            columnas={[
              { clave: "id", titulo: "Pedido", render: (f) => `#${f.id}` },
              { clave: "cliente", titulo: "Cliente", render: (f) => f.cliente },
              { clave: "estado", titulo: "Estado", render: (f) => <StatusBadge tono={f.estado === "Pagado" ? "exito" : "info"}>{f.estado}</StatusBadge> },
              { clave: "total", titulo: "Total", numerica: true, render: (f) => <PriceDisplay monto={f.total} tamano="sm" /> },
            ]}
          />
          <ChartCard
            titulo="Ventas por día (ejemplo)"
            subtitulo="En el panel real se leen de la base de datos."
            datos={[
              { etiqueta: "Lu", valor: 3 },
              { etiqueta: "Ma", valor: 5 },
              { etiqueta: "Mi", valor: 2 },
              { etiqueta: "Ju", valor: 7 },
              { etiqueta: "Vi", valor: 4 },
            ]}
          />
        </div>
      </Bloque>

      <Bloque titulo="Tema oscuro: configurador y paneles" nombre="Sidebar · ComponentSelector · PcSummary">
        <div className="tema-oscuro" style={{ borderRadius: "var(--r-xl)", padding: "var(--s-5)" }}>
          <div className="rejilla" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 280px), 1fr))", alignItems: "start" }}>
            <Sidebar
              grupos={[
                { titulo: "General", enlaces: [{ href: "/diseno", texto: "Dashboard", icono: <LayoutDashboard /> }, { href: "#productos", texto: "Productos", icono: <Package /> }] },
                { titulo: "Operación", enlaces: [{ href: "#inventario", texto: "Inventario", icono: <Boxes /> }, { href: "#pedidos", texto: "Pedidos", icono: <ClipboardList /> }, { href: "#usuarios", texto: "Usuarios", icono: <Users /> }, { href: "#estado", texto: "Estado de servicios", icono: <Gauge /> }] },
              ]}
            />
            <ComponentSelector productos={[EJ_PLACA, EJ_GPU]} seleccionado={elegido} onElegir={setElegido} />
            <PcSummary
              seleccion={{ PROCESADOR: EJ_CPU, PLACA_MADRE: EJ_PLACA }}
              reporte={EJ_REPORTE}
              verificando={false}
              desactualizado={false}
              onVerificar={() => toast("Ejemplo: verificando", "info")}
              onAgregar={() => toast("Ejemplo: agregado")}
              agregado={false}
            />
          </div>
        </div>
      </Bloque>
    </div>
  );
}
