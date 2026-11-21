"use client";

import { Search } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";

/** Búsqueda global: lleva al catálogo con el texto en la URL */
export function SearchBar({ inicial = "", onBuscar }: { inicial?: string; onBuscar?: () => void }) {
  const router = useRouter();
  const [q, setQ] = useState(inicial);
  return (
    <form
      className="buscador"
      role="search"
      onSubmit={(e) => {
        e.preventDefault();
        const texto = q.trim();
        router.push(texto ? `/tienda?q=${encodeURIComponent(texto)}` : "/tienda");
        onBuscar?.();
      }}
    >
      <Search aria-hidden="true" />
      <input
        type="search"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Buscar procesador, placa, RTX…"
        aria-label="Buscar componentes"
      />
    </form>
  );
}
