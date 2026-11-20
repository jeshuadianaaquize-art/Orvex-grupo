# Publicar Orvex con un link permanente (gratis)

```
navegador ──► https://<tu-proyecto>.vercel.app   (web Next.js en Vercel, siempre en línea)
                    │ /api
                    ▼
              https://<tu-dominio>.ngrok-free.app   (dominio fijo y gratuito de ngrok)
                    ▼
              gateway + 9 servicios + RabbitMQ en tu PC (Docker)  ──►  MySQL gratis en Aiven
```

**Limitación:** el link de Vercel no cambia nunca, pero el backend corre en tu PC: la tienda funciona mientras la PC
esté encendida con Docker. Los datos quedan en Aiven aunque apagues la PC.

## 1. Cuentas (las creas tú; todas gratis)

1. **Aiven** (https://aiven.io): crea un servicio **MySQL** con el plan **Free**. En *Overview* copia Host, Port,
   User (`avnadmin`) y Password.
2. **ngrok** (https://ngrok.com): en *Your Authtoken* copia el token; en *Domains* copia tu dominio gratuito
   (termina en `.ngrok-free.app` o `.ngrok-free.dev`).
3. **Vercel** (https://vercel.com): entra con tu cuenta de GitHub.

## 2. Completa tu `.env` (no se sube a git)

```
DB_EXTERNO_HOST=<host de Aiven>
DB_EXTERNO_PUERTO=<puerto de Aiven>
DB_EXTERNO_USUARIO=avnadmin
DB_EXTERNO_CLAVE=<contraseña de Aiven>
NGROK_AUTHTOKEN=<token de ngrok>
NGROK_DOMINIO=<tu-dominio>.ngrok-free.app
WEB_URL_PUBLICA=https://<tu-proyecto>.vercel.app

# Obligatorio en un sitio público: cambia estos secretos (no uses los de ejemplo del repositorio)
JWT_SECRET=<40 o más caracteres al azar>
QR_WEBHOOK_SECRETO=<otro valor al azar>
ORVEX_DEMO_CONTRASENA=<contraseña nueva para las cuentas de demostración>
```

Las cuentas `*@orvex.demo` se crean con `ORVEX_DEMO_CONTRASENA` la primera vez: si dejas la del repositorio,
cualquiera podría entrar como administrador.

## 3. Levanta el backend

```
docker compose -f docker-compose.yml -f docker-compose.nube.yml up -d --build
```

La primera vez crea los 9 esquemas en Aiven y cada servicio sus tablas (Flyway). Comprueba:
`https://<tu-dominio>.ngrok-free.app/api/productos/buscar?tamano=1`.

## 4. Publica la web en Vercel

*Add New → Project →* importa el repositorio `Orvex`:

| Campo | Valor |
|---|---|
| Root Directory | `frontend` |
| Framework | Next.js (lo detecta solo) |
| Variable `API_INTERNA_URL` | `https://<tu-dominio>.ngrok-free.app` |
| Variable `NEXT_PUBLIC_MOSTRAR_DEMO` | `false` (no mostrar las cuentas de demostración en un sitio público) |

Cada push a `main` vuelve a publicar la web sola. `frontend/middleware.ts` agrega la cabecera que evita la página
de advertencia del dominio gratuito de ngrok.
