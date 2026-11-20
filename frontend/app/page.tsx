import { ArrowRight, Boxes, Gauge, ListChecks, MessageSquareWarning, QrCode, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { CategoriasInicio, DatosHero, ProductosInicio } from "./inicio-cliente";

const VENTAJAS = [
  {
    Icono: ShieldCheck,
    titulo: "Compatibilidad verificada",
    texto: "Socket, memoria, formato de placa, medidas de la gráfica y del cooler, antes de pagar.",
  },
  {
    Icono: Gauge,
    titulo: "Consumo y fuente",
    texto: "Estimamos los vatios de tu PC y comprobamos que la fuente alcance con margen de seguridad.",
  },
  {
    Icono: MessageSquareWarning,
    titulo: "Explicaciones claras",
    texto: "Si algo no encaja, te decimos qué pieza causa el problema y por qué.",
  },
  {
    Icono: ListChecks,
    titulo: "Datos técnicos a la vista",
    texto: "Cada producto muestra sus especificaciones y disponibilidad, con precios en bolivianos.",
  },
];

export default function Inicio() {
  return (
    <>
      <section className="hero">
        <div className="contenedor">
          <div>
            <span className="eyebrow">
              Configurador inteligente de PC
            </span>
            <h1>
              Arma tu PC. <em>Nosotros comprobamos</em> que funcione.
            </h1>
            <p className="lead">
              Elige tus componentes y Orvex verifica la compatibilidad y el consumo de energía antes de que compres.
            </p>
            <div className="hero-acciones">
              <Link href="/armador" className="btn btn-primario btn-lg">
                Configurar mi PC <ArrowRight size={18} />
              </Link>
              <Link href="/tienda" className="btn btn-secundario btn-lg">
                Explorar componentes
              </Link>
            </div>
            <DatosHero />
          </div>
          <div className="hero-foto">
            {/* eslint-disable-next-line @next/next/no-img-element -- PNG con transparencia ya optimizado */}
            <img src="/img/hero-gabinete.png" alt="Gabinete gamer con ventiladores de iluminación RGB" width={370} height={432} />
          </div>
        </div>
      </section>

      <section className="beneficios" aria-label="Por qué comprar en Orvex">
        <ul>
          <li><ShieldCheck size={18} aria-hidden="true" /> Compatibilidad verificada antes de comprar</li>
          <li><QrCode size={18} aria-hidden="true" /> Pago con QR en línea o en tienda</li>
          <li><Boxes size={18} aria-hidden="true" /> Stock real, actualizado al instante</li>
        </ul>
      </section>

      <section className="seccion contenedor">
        <div className="seccion-cabecera">
          <div>
            <span className="eyebrow">Catálogo</span>
            <h2>Compra por categoría</h2>
          </div>
          <Link href="/tienda" className="btn btn-fantasma">
            Ver todo <ArrowRight size={16} />
          </Link>
        </div>
        <CategoriasInicio />
      </section>

      <section className="seccion contenedor" style={{ paddingTop: 0 }}>
        <div className="seccion-cabecera">
          <div>
            <span className="eyebrow">Para empezar</span>
            <h2>Opciones más accesibles</h2>
            <p className="texto-2" style={{ marginTop: 6 }}>
              El precio más bajo con stock en cada categoría principal.
            </p>
          </div>
        </div>
        <ProductosInicio />
      </section>

      <section id="como-funciona" className="seccion contenedor" style={{ paddingTop: 0 }}>
        <div className="banda-armador">
          <div>
            <span className="eyebrow">
              Cómo funciona
            </span>
            <h2>De piezas sueltas a una PC que funciona</h2>
            <p style={{ marginBottom: "var(--s-6)" }}>
              Sin conocimientos técnicos avanzados: el configurador te guía categoría por categoría.
            </p>
            <Link href="/armador" className="btn btn-primario btn-lg">
              Empezar mi configuración <ArrowRight size={18} />
            </Link>
          </div>
          <ol>
            <li>
              <span className="num">1</span>
              <span>
                <strong>Elige tus piezas</strong>
                Una por categoría, o solo las que necesites.
              </span>
            </li>
            <li>
              <span className="num">2</span>
              <span>
                <strong>Verifica</strong>
                Revisamos cada regla de compatibilidad y el consumo de la fuente.
              </span>
            </li>
            <li>
              <span className="num">3</span>
              <span>
                <strong>Compra con confianza</strong>
                Si hay una incompatibilidad, la compra se bloquea hasta corregirla.
              </span>
            </li>
          </ol>
        </div>
      </section>

      <section className="seccion contenedor" style={{ paddingTop: 0 }}>
        <div className="seccion-cabecera">
          <div>
            <span className="eyebrow">Por qué Orvex</span>
            <h2>Comprar hardware sin sorpresas</h2>
          </div>
        </div>
        <div className="ventajas entrar">
          {VENTAJAS.map(({ Icono, titulo, texto }) => (
            <div className="tarjeta ventaja" key={titulo}>
              <Icono size={26} />
              <h3>{titulo}</h3>
              <p>{texto}</p>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}
