/** Convierte las especificaciones técnicas (claves del catálogo) en etiquetas legibles. */
type Formato = (v: unknown) => string;

/** Las listas llegan del catálogo separadas por coma ("ATX,mATX,ITX"). */
const lista = (v: unknown) =>
  Array.isArray(v) ? v.join(" · ") : String(v).split(",").map((x) => x.trim()).filter(Boolean).join(" · ");

const ETIQUETAS: Record<string, { nombre: string; formato?: Formato }> = {
  socket: { nombre: "Socket" },
  sockets: { nombre: "Sockets", formato: lista },
  tdp_w: { nombre: "TDP", formato: (v) => `${v} W` },
  tipo_ram: { nombre: "Memoria" },
  capacidad_gb: { nombre: "Capacidad", formato: (v) => (Number(v) >= 1000 ? `${Number(v) / 1000} TB` : `${v} GB`) },
  frecuencia_mhz: { nombre: "Frecuencia", formato: (v) => `${v} MHz` },
  factor_forma: { nombre: "Formato" },
  factores_forma: { nombre: "Formatos", formato: lista },
  largo_mm: { nombre: "Largo", formato: (v) => `${v} mm` },
  altura_mm: { nombre: "Altura", formato: (v) => `${v} mm` },
  max_largo_gpu_mm: { nombre: "GPU máx.", formato: (v) => `${v} mm` },
  max_altura_cooler_mm: { nombre: "Cooler máx.", formato: (v) => `${v} mm` },
  potencia_w: { nombre: "Potencia", formato: (v) => `${v} W` },
  interfaz: { nombre: "Interfaz" },
  nucleos: { nombre: "Núcleos" },
  hilos: { nombre: "Hilos" },
  frecuencia_ghz: { nombre: "Frecuencia", formato: (v) => `${v} GHz` },
  ranuras_ram: { nombre: "Ranuras RAM" },
  ranuras_m2: { nombre: "Ranuras M.2" },
  modulos: { nombre: "Módulos" },
  vram_gb: { nombre: "VRAM", formato: (v) => `${v} GB` },
  certificacion: { nombre: "Certificación" },
  interfaces_almacenamiento: { nombre: "Almacenamiento", formato: lista },
  frecuencia_max_mhz: { nombre: "RAM máx.", formato: (v) => `${v} MHz` },
  conectores_pcie: { nombre: "PCIe 8 pines", formato: (v) => `×${v}` },
  rendimiento: { nombre: "Rendimiento", formato: (v) => `${v}/100` },
};

export interface SpecLegible {
  clave: string;
  nombre: string;
  valor: string;
}

export function specsLegibles(specs: Record<string, unknown> | null | undefined, max = Infinity): SpecLegible[] {
  return Object.entries(specs ?? {})
    .slice(0, max)
    .map(([clave, v]) => {
      const e = ETIQUETAS[clave];
      return {
        clave,
        nombre: e?.nombre ?? clave.replace(/_/g, " "),
        valor: e?.formato ? e.formato(v) : lista(v),
      };
    });
}
