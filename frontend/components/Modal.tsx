"use client";

import { X } from "lucide-react";
import { useEffect, useId, useRef } from "react";

/** Ventana modal accesible */
export function Modal({
  abierto,
  titulo,
  onCerrar,
  children,
  pie,
}: {
  abierto: boolean;
  titulo: string;
  onCerrar: () => void;
  children: React.ReactNode;
  pie?: React.ReactNode;
}) {
  const id = useId();
  const caja = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!abierto) return;
    const anterior = document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    caja.current?.querySelector<HTMLElement>("button, [href], input, select, textarea")?.focus();
    const tecla = (e: KeyboardEvent) => e.key === "Escape" && onCerrar();
    window.addEventListener("keydown", tecla);
    return () => {
      window.removeEventListener("keydown", tecla);
      document.body.style.overflow = overflow;
      anterior?.focus();
    };
  }, [abierto, onCerrar]);

  if (!abierto) return null;
  return (
    <div className="modal-fondo" onMouseDown={(e) => e.target === e.currentTarget && onCerrar()}>
      <div ref={caja} className="modal" role="dialog" aria-modal="true" aria-labelledby={id}>
        <div className="modal-cabecera">
          <h2 id={id}>{titulo}</h2>
          <button className="btn btn-fantasma btn-icono" onClick={onCerrar} aria-label="Cerrar">
            <X size={18} />
          </button>
        </div>
        <div className="modal-cuerpo">{children}</div>
        {pie && <div className="modal-pie">{pie}</div>}
      </div>
    </div>
  );
}

/** Confirmación para acciones que no se pueden deshacer */
export function ConfirmDialog({
  abierto,
  titulo,
  mensaje,
  textoConfirmar = "Confirmar",
  peligro = false,
  onConfirmar,
  onCancelar,
}: {
  abierto: boolean;
  titulo: string;
  mensaje: React.ReactNode;
  textoConfirmar?: string;
  peligro?: boolean;
  onConfirmar: () => void;
  onCancelar: () => void;
}) {
  return (
    <Modal
      abierto={abierto}
      titulo={titulo}
      onCerrar={onCancelar}
      pie={
        <>
          <button className="btn btn-secundario" onClick={onCancelar}>
            Cancelar
          </button>
          <button className={`btn ${peligro ? "btn-peligro" : "btn-primario"}`} onClick={onConfirmar}>
            {textoConfirmar}
          </button>
        </>
      }
    >
      {mensaje}
    </Modal>
  );
}
