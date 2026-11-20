# División del trabajo y subida al repositorio del grupo

El proyecto completo está en el repositorio de Jeshua (`jeshuadianaaquize-art/Orvex`). Cada integrante revisa un bloque
y lo sube **con su propia cuenta**, en **varios commits ordenados por tema**. Cada archivo pertenece a un solo bloque
(`scripts/subir-parte.ps1 -Comprobar` lo verifica).

| Integrante | Bloque | Qué contiene |
|---|---|---|
| **Jeshua** Alexander Aquize Chambilla | Base y plataforma | Repositorio, Maven y librerías comunes, gateway, usuarios y roles, correos, bitácora de acciones, Docker y despliegue, base de la web y panel de administración |
| **Jeremy** José Ibañez Quisbert | Catálogo y armador | Motor y servicio de compatibilidad, catálogo y proveedores, optimizador, tienda, ficha de producto, armador de PC y administración de productos y reglas |
| **Miguel** Gustavo Calle Gonzales | Ventas, inventario y reabastecimiento | Carrito, pago QR, pedidos, punto de venta, inventario, cadenas de Márkov y EOQ, reabastecimiento óptimo, reportes, tablero e integración continua |

## Los commits de cada uno

El script hace un commit por tema, con estos mensajes y con el nombre de quien lo ejecuta como autor:

**Jeshua (10 commits)**
1. chore: configuración inicial del repositorio
2. build: proyecto Maven y librerías comunes del backend
3. feat(gateway): punto de entrada único con control de acceso por rol
4. feat(usuarios): registro, inicio de sesión, roles y recuperación de contraseña
5. feat(notificaciones): avisos por correo de pagos, pedidos y stock
6. feat(bitácora): registro de las acciones importantes del sistema
7. build: contenedores Docker, monitoreo y despliegue
8. feat(web): base de la web, tema visual y componentes compartidos
9. feat(web): cuentas de usuario y panel de administración
10. docs: arquitectura, plan de trabajo y sistema de diseño

**Jeremy (7 commits)**
1. feat(compatibilidad): motor de reglas de compatibilidad
2. feat(catálogo): productos, especificaciones técnicas y proveedores
3. feat(compatibilidad): verificación del armado y configuraciones guardadas
4. feat(optimización): puntaje del armado y alternativas sugeridas
5. feat(web): tienda, ficha de producto y fotos del catálogo
6. feat(web): armador de PC con compatibilidad y optimización
7. feat(web): administración de productos y reglas de compatibilidad

**Miguel (10 commits)**
1. feat(pedidos): carrito, ventas, pago QR, recibos y punto de venta
2. feat(inventario): existencias, movimientos, reservas de stock y alertas
3. feat(modelos): cadenas de Márkov, EOQ y punto de reorden
4. feat(inventario): reabastecimiento óptimo con EOQ, stock de seguridad y Márkov
5. feat(reportes): tablero, reportes exportables y análisis predictivo
6. feat(web): carrito, pago y seguimiento de pedidos
7. feat(web): caja del punto de venta
8. feat(web): panel de inventario y reabastecimiento óptimo
9. feat(web): tablero de ventas y análisis predictivo
10. test: plan de pruebas e integración continua

Si al revisar corriges algo, haz además un commit normal con lo que corregiste.

## El orden

1. **Jeshua** sube primero, directo a `main`: es la base que necesitan los otros dos.
2. **Jeremy** sube en su rama `jeremy` y abre un Pull Request.
3. **Miguel** sube en su rama `miguel` y abre un Pull Request.

Otro integrante revisa cada Pull Request antes de unirlo a `main`.

> Mientras falte un bloque, el proyecto del repositorio del grupo **no compila completo** (el `pom.xml` y el
> `docker-compose.yml` ya nombran todos los servicios). Es normal. Para **probar** mientras revisas, usa el proyecto
> completo del repositorio de Jeshua (paso 0). La integración continua (`.github/`) llega con el bloque de Miguel, así
> no marca errores antes de tiempo.

## 0. Una sola vez: accesos y herramientas

**Accesos (GitHub → Settings → Collaborators → Add people):**

- Quien creó el repositorio del grupo agrega a los otros dos con permiso de escritura.
- Jeshua agrega a Jeremy y a Miguel como colaboradores de `jeshuadianaaquize-art/Orvex` (es privado: sin esto no pueden
  descargar sus bloques).

**En tu computadora:** [Git](https://git-scm.com/download/win) y, para probar, Docker Desktop. Configura tu nombre y el
correo de tu cuenta de GitHub, así los commits salen a tu nombre:

```powershell
git config --global user.name "Tu Nombre"
git config --global user.email "el-correo-de-tu-cuenta-de-github"
```

**Para probar todo mientras revisas** (proyecto completo, en otra carpeta):

```powershell
git clone https://github.com/jeshuadianaaquize-art/Orvex.git Orvex-completo
cd Orvex-completo
copy .env.example .env
docker compose up -d --build
```

La web queda en http://localhost:3000 (las cuentas de prueba están en el README).

## 1. Jeshua: subir la base

```powershell
git clone <direccion-del-repositorio-del-grupo> Orvex-grupo
cd Orvex-grupo
powershell -ExecutionPolicy Bypass -File C:\Users\JESHUA\Desktop\Orvex-main\scripts\subir-parte.ps1 -Integrante Jeshua
git log --oneline
git push -u origin main
```

La dirección es la que GitHub muestra en el botón verde **Code** del repositorio del grupo.

## 2. Jeremy y Miguel: subir su bloque

Cuando el bloque de Jeshua ya está en `main` (y, para Miguel, cuando el de Jeremy ya se unió):

```powershell
git clone <direccion-del-repositorio-del-grupo> Orvex-grupo
cd Orvex-grupo
git checkout -b jeremy
powershell -ExecutionPolicy Bypass -File scripts\subir-parte.ps1 -Integrante Jeremy
git log --oneline
git push -u origin jeremy
```

Miguel hace lo mismo con `miguel` y `-Integrante Miguel`. Para ver antes qué archivos entran en cada commit, sin
cambiar nada, agrega `-Simular` al final del comando.

GitHub muestra un enlace **Compare & pull request**: ábrelo con base `main`, escribe qué revisaste y qué corregiste, y
pide la revisión de otro integrante (*Reviewers*). Cuando lo apruebe, **Merge pull request**.

## Qué revisar en cada bloque

**Todos:** que compile y pasen las pruebas de tus módulos (desde `Orvex-completo/backend`:
`mvn -pl servicios/<servicio> -am verify`, o `mvn verify` para todo), que no haya contraseñas ni claves reales en el
código, que los textos de la pantalla estén bien escritos y que la documentación de tu bloque (`docs/`) describa lo que
el código hace de verdad.

**Jeshua — Base y plataforma**

- Registro, inicio de sesión, recuperar y restablecer contraseña; bloqueo por intentos fallidos.
- Roles (cliente, vendedor, almacenero, administrador): el gateway (`ReglasAcceso`) y cada servicio niegan lo que no
  corresponde. Prueba entrar a `/admin` con una cuenta de cliente.
- Correos y bitácora de acciones: cada acción importante deja rastro.
- `docker-compose.yml`, `.env.example`, `Jenkinsfile`, `infra/` y la guía `docs/publicar.md`.
- La web en celular, tablet y computadora: menú, portada, colores del tema y contraste.

**Jeremy — Catálogo y armador**

- Búsqueda, filtros, orden y paginación de la tienda; ficha de producto con especificaciones y fotos.
- Panel de productos (crear, editar, publicar, fotos) y proveedores.
- Las reglas de compatibilidad (socket, memoria, tamaño, consumo y fuente): arma PCs correctas e incorrectas y mira que
  avise bien.
- El optimizador: sugerencias de mejora y alternativas más baratas con sentido.

**Miguel — Ventas, inventario y reabastecimiento**

- Carrito, pago por QR, reserva de stock (que dos compras no se lleven la última unidad) y seguimiento del pedido.
- Punto de venta y ventas de tienda; anulaciones y efectivo.
- Inventario: ingresos, ajustes, alertas de stock bajo.
- Reabastecimiento óptimo: con el filtro «Datos simulados del informe» deben salir las Tablas 17, 18 y 19 del informe
  de IO II (Q* = 54 para la RAM, SSD en Reordenar con 8,1 %, ahorro de Bs 11.024).
- Tablero, reportes y exportación.

## Si algo sale mal

- **«Tienes cambios sin guardar»**: el script no sigue si hay cambios pendientes. Haz commit o `git stash`.
- **«Configura tu nombre y correo»**: haz los dos `git config` del paso 0.
- **«No se pudo descargar el repositorio de origen»**: falta el acceso de colaborador al repositorio de Jeshua, o
  GitHub pide iniciar sesión (se abre una ventana del navegador la primera vez).
- **Algo salió mal antes del push**: `git reset --hard origin/main` (en tu rama) deja todo como estaba; vuelve a
  ejecutar el script.
