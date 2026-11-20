"use client";

import { useEffect, useMemo, useState } from "react";
import { CategoryCard } from "@/components/CategoryCard";
import { ErrorState, ProductCardSkeleton } from "@/components/Estados";
import { ProductCard } from "@/components/ProductCard";
import { useToast } from "@/components/Toast";
import { CATEGORIAS, listarProductos, type Categoria, type Producto } from "@/lib/api";
import { useCart } from "@/lib/cart";

const DESCRIPCION: Record<Categoria, string> = {
  PROCESADOR: "El cerebro de tu PC",
  PLACA_MADRE: "Conecta todas las piezas",
  RAM: "Memoria para multitarea",
  TARJETA_GRAFICA: "Juegos, edición y 3D",
  ALMACENAMIENTO: "SSD NVMe y discos",
  FUENTE: "Energía estable y segura",
  GABINETE: "Espacio y ventilación",
  REFRIGERACION: "Temperaturas bajo control",
};

const DESTACADAS: Categoria[] = ["PROCESADOR", "TARJETA_GRAFICA", "PLACA_MADRE", "RAM"];

/** Una sola carga del catálogo compartida por las secciones del inicio. */
let promesa: Promise<Producto[]> | null = null;
function useCatalogo() {
  const [productos, setProductos] = useState<Producto[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [intento, setIntento] = useState(0);
  useEffect(() => {
    let vivo = true;
    promesa ??= listarProductos();
    promesa
      .then((p) => vivo && setProductos(p))
      .catch((e: Error) => {
        promesa = null;
        if (vivo) setError(e.message);
      });
    return () => {
      vivo = false;
    };
  }, [intento]);
  const reintentar = () => {
    setError(null);
    setIntento((i) => i + 1);
  };
  return { productos, error, reintentar };
}

export function DatosHero() {
  const { productos } = useCatalogo();
  const marcas = useMemo(() => new Set(productos?.map((p) => p.marca).filter(Boolean)).size, [productos]);
  return (
    <div className="hero-datos">
      <div>
        <strong>{productos ? productos.length : "—"}</strong>
        componentes en catálogo
      </div>
      <div>
        <strong>{productos ? marcas : "—"}</strong>
        marcas
      </div>
      <div>
        <strong>{CATEGORIAS.length}</strong>
        categorías
      </div>
    </div>
  );
}

export function CategoriasInicio() {
  const { productos, error, reintentar } = useCatalogo();
  if (error) return <ErrorState mensaje={error} onReintentar={reintentar} />;
  return (
    <div className="rejilla entrar" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(min(100%, 260px), 1fr))" }}>
      {CATEGORIAS.map((c) => {
        const de = productos?.filter((p) => p.categoria === c.id) ?? [];
        return (
          <CategoryCard
            key={c.id}
            categoria={c.id}
            nombre={c.etiqueta}
            descripcion={DESCRIPCION[c.id]}
            cantidad={productos ? de.length : undefined}
            desde={de.length ? Math.min(...de.map((p) => p.precio)) : undefined}
          />
        );
      })}
    </div>
  );
}

export function ProductosInicio() {
  const { productos, error, reintentar } = useCatalogo();
  const { agregarVarios } = useCart();
  const toast = useToast();
  const [agregados, setAgregados] = useState<number[]>([]);

  if (error) return <ErrorState mensaje={error} onReintentar={reintentar} />;
  if (!productos) {
    return (
      <div className="rejilla">
        {DESTACADAS.map((c) => (
          <ProductCardSkeleton key={c} />
        ))}
      </div>
    );
  }
  const elegidos = DESTACADAS.map((c) =>
    productos.filter((p) => p.categoria === c && p.enStock !== false).sort((a, b) => a.precio - b.precio)[0],
  ).filter(Boolean) as Producto[];

  return (
    <div className="rejilla entrar">
      {elegidos.map((p) => (
        <ProductCard
          key={p.id}
          producto={p}
          agregado={agregados.includes(p.id)}
          onAgregar={(prod) => {
            void agregarVarios([prod]).then((ok) => {
              if (!ok) return;
              setAgregados((a) => [...a, prod.id]);
              toast(`${prod.nombre} se agregó al carrito`);
            });
          }}
        />
      ))}
    </div>
  );
}
