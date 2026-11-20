"use client";

import { CircleCheck, CircleX, Info } from "lucide-react";
import { createContext, useCallback, useContext, useRef, useState } from "react";

type TipoToast = "exito" | "error" | "info";
interface Toast {
  id: number;
  tipo: TipoToast;
  mensaje: string;
}

const ToastCtx = createContext<((mensaje: string, tipo?: TipoToast) => void) | null>(null);
const ICONO = { exito: CircleCheck, error: CircleX, info: Info };

/** Avisos breves que no bloquean la navegación (p. ej. "Agregado al carrito"). */
export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const siguiente = useRef(0);

  const mostrar = useCallback((mensaje: string, tipo: TipoToast = "exito") => {
    const id = ++siguiente.current;
    setToasts((t) => [...t.slice(-2), { id, tipo, mensaje }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3200);
  }, []);

  return (
    <ToastCtx.Provider value={mostrar}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {toasts.map((t) => {
          const Icono = ICONO[t.tipo];
          return (
            <div key={t.id} className={`toast toast-${t.tipo}`}>
              <Icono aria-hidden="true" />
              <span>{t.mensaje}</span>
            </div>
          );
        })}
      </div>
    </ToastCtx.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastCtx);
  if (!ctx) throw new Error("useToast debe usarse dentro de ToastProvider");
  return ctx;
}
