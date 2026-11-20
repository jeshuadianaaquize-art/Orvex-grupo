import { CircleCheck } from "lucide-react";

/** Marco común de las pantallas de cuenta: formulario a la izquierda y beneficios a la derecha. */
export function AuthLayout({
  titulo,
  subtitulo,
  children,
}: {
  titulo: string;
  subtitulo?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="contenedor auth">
      <section className="tarjeta auth-tarjeta" aria-labelledby="titulo-auth">
        <h1 id="titulo-auth">{titulo}</h1>
        {subtitulo && <p className="texto-2">{subtitulo}</p>}
        {children}
      </section>
      <aside className="auth-lado" aria-hidden="true">
        <span className="eyebrow" style={{ color: "var(--cian-500)" }}>
          Tu cuenta Orvex
        </span>
        <h2>Arma, verifica y compra con confianza.</h2>
        <ul>
          {[
            "Guarda tus direcciones de entrega",
            "Sigue el estado de tus pedidos",
            "Recibe avisos de pago y entrega por correo",
          ].map((t) => (
            <li key={t}>
              <CircleCheck size={18} /> {t}
            </li>
          ))}
        </ul>
      </aside>
    </div>
  );
}
