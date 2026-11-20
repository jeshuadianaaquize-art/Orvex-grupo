# Seguridad, sesiones y roles (M02)

Implementa HU-01, HU-02, HU-03 y HU-19 (RF-01, RF-02, RF-21; RNF-04).

## Cómo funciona una petición con sesión

```
Navegador ── Authorization: Bearer <JWT> ──► gateway
  1. Borra X-Usuario-Id / X-Usuario-Rol si vienen del cliente (no se pueden falsificar).
  2. Valida el JWT: firma HMAC-SHA256, vencimiento, emisor y que no se haya cerrado la sesión.
  3. Pregunta a usuarios (/internal/usuarios/{id}/acceso, caché 30 s) si la cuenta sigue activa y su rol actual.
  4. Aplica la regla de la ruta (pública / con sesión / por rol) → 401 o 403 con {"error": "..."}.
  5. Reenvía la petición con X-Usuario-Id y X-Usuario-Rol.
                                   ──► servicio (lee la identidad de esas cabeceras)
```

Las rutas de administración vuelven a comprobar el rol dentro del servicio (defensa en profundidad):
ni con la ruta directa se salta el permiso, como pide el caso de uso "Gestionar usuarios y roles".

## Reglas de acceso (`gateway/.../ReglasAcceso.java`)

| Ruta | Acceso |
|---|---|
| `GET /api/productos/**`, `GET /api/categorias/**`, `POST /api/armador/verificar` | Público |
| `POST /api/auth/registro`, `login`, `recuperar`, `restablecer` | Público |
| `POST /api/auth/logout`, `/api/usuarios/me/**` | Con sesión |
| `/api/usuarios/**`, `/api/roles/**`, `/api/estado`, `/api/auditoria/**`, `/api/reportes/**` | Administrador |
| Cualquier otra ruta `/api/**` | Con sesión (cada etapa agrega las reglas de su rol) |

## API del servicio de usuarios

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/registro` | `{nombre, correo, contrasena, telefono?}` → 201 con la sesión iniciada. Rol Cliente |
| POST | `/api/auth/login` | `{correo, contrasena}` → `{token, expira, usuario}` |
| POST | `/api/auth/logout` | Invalida el token actual (204) |
| POST | `/api/auth/recuperar` | `{correo}` → siempre el mismo mensaje |
| POST | `/api/auth/restablecer` | `{token, contrasena}` |
| GET/PUT | `/api/usuarios/me` | Ver / editar nombre y teléfono |
| POST | `/api/usuarios/me/contrasena` | `{actual, nueva}` |
| GET/POST | `/api/usuarios/me/direcciones` | Listar / agregar (máximo 10) |
| PUT/DELETE | `/api/usuarios/me/direcciones/{id}` | Solo las propias (404 si es de otro usuario) |
| GET | `/api/usuarios?buscar=&rol=&estado=&pagina=` | Admin: lista paginada (20 por página) |
| PATCH | `/api/usuarios/{id}` | Admin: `{rol?, estado?}`. No puede cambiarse a sí mismo |
| GET | `/api/roles` | Admin: los 4 roles |

Errores: `{"error": "mensaje", "campos": {"campo": "mensaje"}}`. Códigos: 400 datos inválidos, 401 sin sesión o
credenciales incorrectas, 403 sin permiso o cuenta bloqueada, 409 correo ya registrado, 429 demasiados intentos.

## Controles aplicados (OWASP Top 10)

| Riesgo | Control |
|---|---|
| A01 Control de acceso | Reglas en el gateway + comprobación en el servicio; el id del usuario sale del token, nunca de la URL; direcciones filtradas por dueño |
| A02 Criptografía | bcrypt (costo 10); JWT HS256 con secreto de 32+ caracteres por variable de entorno; nunca se guardan tarjetas |
| A03 Inyección | JPA con parámetros; validación de entrada con Bean Validation en el servidor |
| A04 Diseño inseguro | Mismo mensaje para correo inexistente y contraseña incorrecta; recuperación sin revelar cuentas |
| A05 Configuración | Sin `/actuator` expuesto por el gateway; API interna `/internal/**` sin ruta pública; CORS solo para la web |
| A07 Autenticación | Bloqueo de 15 min tras 5 intentos fallidos por correo; comparación en tiempo constante aunque el correo no exista; tokens con vencimiento (60 min) y revocables |
| A09 Registro | Cambios de rol y de estado con usuario responsable en el log `AUDITORIA` (pasa al servicio de auditoría en M07) |
| Redirección abierta | `?siguiente=` solo acepta rutas internas |

## Decisiones (continúan las del plan)

| # | Tema | Decisión |
|---|---|---|
| D14 | Enlace de recuperación | El diccionario no tiene columnas para un token de recuperación. Se usa un JWT firmado que vence en 30 min y lleva una huella del hash actual de la contraseña: al cambiarla, el enlace deja de servir (un solo uso). No requiere tablas nuevas |
| D15 | Cerrar sesión | El gateway guarda en memoria los `jti` cerrados hasta su vencimiento. Limitación: si el gateway se reinicia o hay varias réplicas, un token cerrado podría valer hasta que venza (máx. 60 min). Mejora futura: almacén compartido (Redis) |
| D16 | Bloqueos y cambios de rol | Se aplican en menos de 30 s sin esperar el vencimiento del token (consulta con caché). Si el servicio de usuarios cae, el gateway usa el rol del token (modo degradado, RNF-03) |
| D17 | Token en el navegador | Se guarda en `localStorage` para que la sesión sobreviva a recargas. Con un dominio propio conviene migrar a cookie `HttpOnly` + `SameSite` |
| D18 | Correos | Bienvenida y recuperación se envían en segundo plano por SMTP (Mailpit en desarrollo). En M07 pasan a la cola de mensajes y al servicio de Notificaciones con reintentos |
| D19 | Auditoría | Hasta M07 queda en el log `AUDITORIA` del servicio de usuarios; luego se publica como evento al servicio de Auditoría |

## Cuentas de demostración

Con `ORVEX_DEMO=true` (valor por defecto en Docker Compose de desarrollo) se crean al arrancar, una por rol:
`administrador@orvex.demo`, `vendedor@orvex.demo`, `almacenero@orvex.demo` y `cliente@orvex.demo`,
con la contraseña `ORVEX_DEMO_CONTRASENA` (por defecto `Orvex2026!`). **Nunca habilitar en producción.**

Correos de prueba: http://localhost:8025 (Mailpit).

## Verificación (2026-11-07)

Pruebas automáticas: 23 nuevas (JWT 7, cuentas 10, web de usuarios 6) y 16 del gateway; 63 en total en verde.

En Docker, a través del gateway (script de punta a punta):
registro con errores por campo · correo repetido 409 · mismo mensaje para contraseña incorrecta y correo inexistente ·
perfil y direcciones · cliente sin acceso a rutas de admin aun falsificando cabeceras · admin no puede cambiarse a sí
mismo · cuenta bloqueada pierde el acceso con su token vigente y no puede ingresar · recuperación por correo
(enlace recibido en Mailpit) · el enlace sirve una sola vez · logout invalida el token · bloqueo tras 5 intentos ·
contraseña guardada como hash `$2a$10$` · nombres con tildes y ñ guardados en UTF-8 · auditoría en el log.

En el navegador: ingreso del admin que lleva al panel, cambio de rol con confirmación, estado de servicios,
cierre de sesión, redirección a "Ingresar" para visitantes y "Sin acceso" para un cliente en el panel.
