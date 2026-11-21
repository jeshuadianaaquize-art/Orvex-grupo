const formato = new Intl.NumberFormat("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Importe en bolivianos. El símbolo va más pequeño para que el número se lea primero. */
export function PriceDisplay({
  monto,
  tamano = "md",
  prefijo,
}: {
  monto: number;
  tamano?: "sm" | "md" | "lg";
  prefijo?: string;
}) {
  return (
    <span className={`precio ${tamano === "md" ? "" : `precio-${tamano}`}`}>
      {prefijo && <small>{prefijo} </small>}
      <small>{"Bs "}</small>
      {formato.format(monto)}
    </span>
  );
}
