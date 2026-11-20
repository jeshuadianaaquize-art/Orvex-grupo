"use client";

import { Boxes, BrainCircuit, Gauge, LayoutDashboard, Package, Receipt, ScrollText, Users, Workflow } from "lucide-react";
import { RequiereSesion } from "@/components/RequiereSesion";
import { Sidebar } from "@/components/Sidebar";

/** Panel de administración */
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequiereSesion roles={["ADMINISTRADOR"]}>
      <div className="tema-oscuro panel">
        <Sidebar
          etiqueta="Panel de administración"
          grupos={[
            {
              titulo: "Negocio",
              enlaces: [
                { href: "/admin", texto: "Tablero", icono: <LayoutDashboard /> },
                { href: "/admin/pedidos", texto: "Ventas", icono: <Receipt /> },
                { href: "/admin/investigacion", texto: "Análisis predictivo", icono: <BrainCircuit /> },
              ],
            },
            {
              titulo: "Catálogo",
              enlaces: [
                { href: "/admin/productos", texto: "Productos", icono: <Package /> },
                { href: "/inventario", texto: "Inventario", icono: <Boxes /> },
                { href: "/admin/reglas", texto: "Reglas de compatibilidad", icono: <Workflow /> },
              ],
            },
            { titulo: "Acceso", enlaces: [{ href: "/admin/usuarios", texto: "Usuarios y roles", icono: <Users /> }] },
            {
              titulo: "Sistema",
              enlaces: [
                { href: "/admin/auditoria", texto: "Auditoría", icono: <ScrollText /> },
                { href: "/admin/estado", texto: "Estado de servicios", icono: <Gauge /> },
              ],
            },
          ]}
        />
        <div className="panel-contenido">{children}</div>
      </div>
    </RequiereSesion>
  );
}
