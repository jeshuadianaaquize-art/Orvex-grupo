import { CircuitBoard, Cpu, Fan, HardDrive, MemoryStick, PcCase, Zap, type LucideProps } from "lucide-react";
import type { Categoria } from "@/lib/api";

/** Tarjeta gráfica: lucide-react no trae este icono */
export function IconoGpu({ size = 24, strokeWidth = 2, className, ...resto }: LucideProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
      {...resto}
    >
      <rect x="2" y="6" width="20" height="11" rx="2" />
      <circle cx="8" cy="11.5" r="2.5" />
      <circle cx="16" cy="11.5" r="2.5" />
      <path d="M5 17v2M9 17v2M13 17v2" />
      <path d="M2 9H1" />
    </svg>
  );
}

const ICONOS: Record<Categoria, (p: LucideProps) => JSX.Element> = {
  PROCESADOR: (p) => <Cpu {...p} />,
  PLACA_MADRE: (p) => <CircuitBoard {...p} />,
  RAM: (p) => <MemoryStick {...p} />,
  TARJETA_GRAFICA: (p) => <IconoGpu {...p} />,
  ALMACENAMIENTO: (p) => <HardDrive {...p} />,
  FUENTE: (p) => <Zap {...p} />,
  GABINETE: (p) => <PcCase {...p} />,
  REFRIGERACION: (p) => <Fan {...p} />,
};

export function IconoCategoria({ categoria, ...p }: { categoria: Categoria } & LucideProps) {
  const Icono = ICONOS[categoria];
  return <Icono aria-hidden="true" {...p} />;
}

/** Isotipo de Orvex: un chip con una órbita, en los colores de la marca. */
export function LogoMarca({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" className={className} aria-hidden="true">
      <rect x="1" y="1" width="30" height="30" rx="9" fill="#0b1020" />
      <rect x="9" y="9" width="14" height="14" rx="3.5" fill="none" stroke="#3b82f6" strokeWidth="2.4" />
      <circle cx="16" cy="16" r="2.6" fill="#06b6d4" />
      <path d="M16 4v4M16 24v4M4 16h4M24 16h4" stroke="#3b82f6" strokeWidth="2.2" strokeLinecap="round" />
    </svg>
  );
}
