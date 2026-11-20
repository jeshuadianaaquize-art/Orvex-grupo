<#
.SYNOPSIS
  Sube a este repositorio el bloque de Orvex de un integrante (docs/division-del-trabajo.md), en varios commits
  ordenados por tema y con tu propio nombre de Git como autor.

.DESCRIPTION
  Se ejecuta dentro de un clon del repositorio del grupo. Trae del repositorio de origen los archivos del integrante,
  grupo por grupo (por ejemplo: gateway, usuarios, notificaciones, web...), y hace un commit por cada grupo. También
  borra los archivos viejos de esos grupos que ya no existen en el origen. No hace push: primero revisas, corriges (con
  commits normales) y después subes.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\subir-parte.ps1 -Integrante Jeremy

.EXAMPLE
  # Solo muestra los grupos y sus archivos, sin tocar nada.
  powershell -ExecutionPolicy Bypass -File scripts\subir-parte.ps1 -Integrante Jeremy -Simular

.EXAMPLE
  # Comprueba que cada archivo del proyecto pertenece a un solo grupo (lo usa quien mantiene la división).
  powershell -ExecutionPolicy Bypass -File scripts\subir-parte.ps1 -Comprobar -Referencia HEAD
#>
param(
    [ValidateSet("Jeshua", "Jeremy", "Miguel")]
    [string]$Integrante,
    [string]$Fuente = "https://github.com/jeshuadianaaquize-art/Orvex.git",
    [string]$Rama = "main",
    [switch]$Simular,
    [switch]$Comprobar,
    [string]$Referencia
)

$ErrorActionPreference = "Stop"

# ------------------------------------------------------------------ la división
# Cada grupo = un commit, en este orden. Una ruta que termina en / es una carpeta. Un archivo va al grupo cuya ruta
# coincide de forma MÁS LARGA (así un archivo puntual puede ir a otro grupo que su carpeta).
$Grupos = @(
    # ---------------- Jeshua: base y plataforma
    @{ I = "Jeshua"; M = "chore: configuración inicial del repositorio"; R = @(
        "README.md", ".gitignore", ".gitattributes", ".env.example", "scripts/",
        "backend/api/", "Orvex") },   # restos del primer prototipo: solo se borran
    @{ I = "Jeshua"; M = "build: proyecto Maven y librerías comunes del backend"; R = @(
        "backend/pom.xml", "backend/Dockerfile", "backend/.dockerignore",
        "backend/common/", "backend/eventos/", "backend/seguridad-jwt/") },
    @{ I = "Jeshua"; M = "feat(gateway): punto de entrada único con control de acceso por rol"; R = @(
        "backend/gateway/") },
    @{ I = "Jeshua"; M = "feat(usuarios): registro, inicio de sesión, roles y recuperación de contraseña"; R = @(
        "backend/servicios/usuarios/", "docs/seguridad.md", "docs/openapi/usuarios.json") },
    @{ I = "Jeshua"; M = "feat(notificaciones): avisos por correo de pagos, pedidos y stock"; R = @(
        "backend/servicios/notificaciones/", "docs/openapi/notificaciones.json") },
    @{ I = "Jeshua"; M = "feat(bitácora): registro de las acciones importantes del sistema"; R = @(
        "backend/servicios/auditoria/", "docs/openapi/auditoria.json") },
    @{ I = "Jeshua"; M = "build: contenedores Docker, monitoreo y despliegue"; R = @(
        "docker-compose.yml", "docker-compose.nube.yml", "Jenkinsfile", "infra/",
        "docs/publicar.md", "docs/eventos-y-resiliencia.md") },
    @{ I = "Jeshua"; M = "feat(web): base de la web, tema visual y componentes compartidos"; R = @(
        "frontend/.dockerignore", "frontend/.env.example", "frontend/.gitignore", "frontend/Dockerfile",
        "frontend/middleware.ts", "frontend/next.config.mjs", "frontend/package.json", "frontend/package-lock.json",
        "frontend/tsconfig.json", "frontend/public/img/hero-gabinete.png",
        "frontend/app/layout.tsx", "frontend/app/page.tsx", "frontend/app/inicio-cliente.tsx",
        "frontend/app/globals.css", "frontend/app/neon.css", "frontend/app/carbon.css", "frontend/app/paneles.css",
        "frontend/components/AuthLayout.tsx", "frontend/components/Badges.tsx", "frontend/components/DataTable.tsx",
        "frontend/components/Estados.tsx", "frontend/components/Footer.tsx", "frontend/components/FormField.tsx",
        "frontend/components/Modal.tsx", "frontend/components/Navbar.tsx", "frontend/components/Pagination.tsx",
        "frontend/components/RequiereSesion.tsx", "frontend/components/Sidebar.tsx", "frontend/components/Toast.tsx",
        "frontend/components/iconos.tsx", "frontend/lib/api.ts", "frontend/lib/auth.tsx") },
    @{ I = "Jeshua"; M = "feat(web): cuentas de usuario y panel de administración"; R = @(
        "frontend/app/ingresar/", "frontend/app/registro/", "frontend/app/recuperar/", "frontend/app/restablecer/",
        "frontend/app/cuenta/", "frontend/app/admin/layout.tsx", "frontend/app/admin/usuarios/",
        "frontend/app/admin/auditoria/", "frontend/app/admin/estado/", "frontend/lib/admin.ts") },
    @{ I = "Jeshua"; M = "docs: arquitectura, plan de trabajo y sistema de diseño"; R = @(
        "docs/arquitectura.md", "docs/plan-implementacion.md", "docs/sistema-diseno.md", "docs/division-del-trabajo.md") },

    # ---------------- Jeremy: catálogo y armador
    @{ I = "Jeremy"; M = "feat(compatibilidad): motor de reglas de compatibilidad"; R = @(
        "backend/compat-engine/") },
    @{ I = "Jeremy"; M = "feat(catálogo): productos, especificaciones técnicas y proveedores"; R = @(
        "backend/servicios/catalogo/", "docs/catalogo.md", "docs/openapi/catalogo.json") },
    @{ I = "Jeremy"; M = "feat(compatibilidad): verificación del armado y configuraciones guardadas"; R = @(
        "backend/servicios/compatibilidad/", "docs/compatibilidad.md", "docs/openapi/compatibilidad.json") },
    @{ I = "Jeremy"; M = "feat(optimización): puntaje del armado y alternativas sugeridas"; R = @(
        "backend/servicios/optimizacion/", "docs/optimizacion.md", "docs/openapi/optimizacion.json") },
    @{ I = "Jeremy"; M = "feat(web): tienda, ficha de producto y fotos del catálogo"; R = @(
        "frontend/app/tienda/", "frontend/app/producto/", "frontend/app/diseno/", "frontend/app/creditos/",
        "frontend/components/CategoryCard.tsx", "frontend/components/FilterPanel.tsx",
        "frontend/components/ImagenProducto.tsx", "frontend/components/PriceDisplay.tsx",
        "frontend/components/ProductCard.tsx", "frontend/components/SearchBar.tsx",
        "frontend/lib/creditos-fotos.ts", "frontend/public/img/productos/", "frontend/public/img/componentes/") },
    @{ I = "Jeremy"; M = "feat(web): armador de PC con compatibilidad y optimización"; R = @(
        "frontend/app/armador/", "frontend/components/ComponentSelector.tsx",
        "frontend/components/OptimizationPanel.tsx", "frontend/components/PcSummary.tsx", "frontend/lib/specs.ts") },
    @{ I = "Jeremy"; M = "feat(web): administración de productos y reglas de compatibilidad"; R = @(
        "frontend/app/admin/productos/", "frontend/app/admin/reglas/", "frontend/components/EditorProducto.tsx") },

    # ---------------- Miguel: ventas, inventario y modelos de reabastecimiento
    @{ I = "Miguel"; M = "feat(pedidos): carrito, ventas, pago QR, recibos y punto de venta"; R = @(
        "backend/servicios/pedidos-pagos/", "docs/pedidos.md", "docs/openapi/pedidos-pagos.json") },
    @{ I = "Miguel"; M = "feat(inventario): existencias, movimientos, reservas de stock y alertas"; R = @(
        "backend/servicios/inventario/", "docs/inventario.md", "docs/openapi/inventario.json") },
    @{ I = "Miguel"; M = "feat(modelos): cadenas de Márkov, EOQ y punto de reorden"; R = @(
        "backend/investigacion-operativa/") },
    @{ I = "Miguel"; M = "feat(inventario): reabastecimiento óptimo con EOQ, stock de seguridad y Márkov"; R = @(
        "backend/servicios/inventario/src/main/resources/db/migration/V4__reabastecimiento_optimo.sql",
        "backend/servicios/inventario/src/main/resources/db/migration/V5__reabastecimiento_resto_catalogo.sql",
        "backend/servicios/inventario/src/main/java/com/orvex/inventario/modelo/ConfiguracionReabastecimiento.java",
        "backend/servicios/inventario/src/main/java/com/orvex/inventario/modelo/ParametroInventario.java",
        "backend/servicios/inventario/src/main/java/com/orvex/inventario/modelo/TransicionEstado.java",
        "backend/servicios/inventario/src/main/java/com/orvex/inventario/servicio/ReabastecimientoService.java",
        "backend/servicios/inventario/src/main/java/com/orvex/inventario/web/ReabastecimientoController.java",
        "backend/servicios/inventario/src/test/java/com/orvex/inventario/ReabastecimientoMysqlTest.java",
        "docs/investigacion-operativa.md") },
    @{ I = "Miguel"; M = "feat(reportes): tablero, reportes exportables y análisis predictivo"; R = @(
        "backend/servicios/reportes/", "docs/reportes.md", "docs/openapi/reportes.json") },
    @{ I = "Miguel"; M = "feat(web): carrito, pago y seguimiento de pedidos"; R = @(
        "frontend/app/carrito/", "frontend/app/checkout/", "frontend/app/pedidos/",
        "frontend/components/CartItem.tsx", "frontend/lib/cart.tsx", "frontend/lib/pedidos.ts") },
    @{ I = "Miguel"; M = "feat(web): caja del punto de venta"; R = @(
        "frontend/app/pos/") },
    @{ I = "Miguel"; M = "feat(web): panel de inventario y reabastecimiento óptimo"; R = @(
        "frontend/app/inventario/", "frontend/components/RegistrarMovimiento.tsx",
        "frontend/lib/inventario.ts", "frontend/lib/reabastecimiento.ts") },
    @{ I = "Miguel"; M = "feat(web): tablero de ventas y análisis predictivo"; R = @(
        "frontend/app/admin/page.tsx", "frontend/app/admin/pedidos/", "frontend/app/admin/investigacion/",
        "frontend/components/ChartCard.tsx", "frontend/components/GraficoLineas.tsx",
        "frontend/components/MatrizMarkov.tsx", "frontend/lib/markov.ts") },
    @{ I = "Miguel"; M = "test: plan de pruebas e integración continua"; R = @(
        "docs/pruebas.md", ".github/") }
)

# Índice del grupo de un archivo (la ruta que coincide de forma más larga), o -1.
function Get-Grupo([string]$archivo) {
    $mejor = -1
    $largo = -1
    for ($g = 0; $g -lt $Grupos.Count; $g++) {
        foreach ($r in $Grupos[$g].R) {
            $coincide = if ($r.EndsWith("/")) { $archivo.StartsWith($r) } else { $archivo -eq $r }
            if ($coincide -and $r.Length -gt $largo) {
                $mejor = $g
                $largo = $r.Length
            }
        }
    }
    return $mejor
}

function Get-Archivos([string]$ref) {
    $lista = git -c core.quotepath=false ls-tree -r --name-only $ref
    if ($LASTEXITCODE -ne 0) { throw "No se pudo leer '$ref'." }
    return @($lista | Where-Object { $_ })
}

if (-not (Test-Path ".git")) { throw "Ejecuta el script en la carpeta raíz del repositorio (donde está la carpeta .git)." }

# ------------------------------------------------------------------ modo comprobación
if ($Comprobar) {
    if (-not $Referencia) { $Referencia = "HEAD" }
    $todos = Get-Archivos $Referencia
    $sinGrupo = @($todos | Where-Object { (Get-Grupo $_) -lt 0 })
    foreach ($nombre in "Jeshua", "Jeremy", "Miguel") {
        $n = @($todos | Where-Object { $g = Get-Grupo $_; $g -ge 0 -and $Grupos[$g].I -eq $nombre }).Count
        $c = @($Grupos | Where-Object { $_.I -eq $nombre }).Count
        Write-Host ("{0}: {1} archivos en {2} commits" -f $nombre, $n, $c)
    }
    if ($sinGrupo.Count) {
        $sinGrupo | ForEach-Object { Write-Host "  sin grupo: $_" }
        throw "$($sinGrupo.Count) archivos sin grupo."
    }
    Write-Host "Cada archivo está en un solo grupo (total $($todos.Count))."
    exit 0
}

if (-not $Integrante) { throw "Indica quién sube: -Integrante Jeshua, -Integrante Jeremy o -Integrante Miguel." }

# ------------------------------------------------------------------ preparar
if (-not $Simular) {
    if (git status --porcelain) { throw "Tienes cambios sin guardar. Haz commit o descártalos antes de seguir." }
    $nombreGit = git config user.name
    $correoGit = git config user.email
    if (-not $nombreGit -or -not $correoGit) {
        throw "Configura tu nombre y correo de GitHub: git config --global user.name `"Tu Nombre`" y git config --global user.email `"tu-correo`"."
    }
    Write-Host "Los commits saldrán a nombre de: $nombreGit <$correoGit>"
}
if (@(git remote) -notcontains "origen-orvex") { git remote add origen-orvex $Fuente }
Write-Host "Descargando $Fuente ($Rama)..."
git fetch -q origen-orvex $Rama
if ($LASTEXITCODE -ne 0) { throw "No se pudo descargar el repositorio de origen. ¿Te dieron acceso?" }
$ref = "origen-orvex/$Rama"

$origen = Get-Archivos $ref
$actuales = @(git -c core.quotepath=false ls-files 2>$null | Where-Object { $_ })
$mensajeTmp = Join-Path $env:TEMP "orvex-commit.txt"
$utf8 = New-Object System.Text.UTF8Encoding $false
$hechos = 0

for ($g = 0; $g -lt $Grupos.Count; $g++) {
    if ($Grupos[$g].I -ne $Integrante) { continue }
    $nuevos = @($origen | Where-Object { (Get-Grupo $_) -eq $g })
    $sobran = @($actuales | Where-Object { ((Get-Grupo $_) -eq $g) -and ($origen -notcontains $_) })
    Write-Host ""
    Write-Host ("{0}  ({1} archivos, {2} a borrar)" -f $Grupos[$g].M, $nuevos.Count, $sobran.Count)
    if ($Simular) {
        $nuevos | ForEach-Object { Write-Host "    $_" }
        continue
    }
    # De a 50 archivos para no pasar el largo máximo de una línea de comandos en Windows.
    for ($i = 0; $i -lt $nuevos.Count; $i += 50) {
        $parte = $nuevos[$i..([Math]::Min($i + 49, $nuevos.Count - 1))]
        git checkout $ref -- $parte
        if ($LASTEXITCODE -ne 0) { throw "No se pudieron copiar los archivos." }
    }
    foreach ($a in $sobran) {
        git rm -q -r --cached -- $a | Out-Null
        if (Test-Path -LiteralPath $a) { Remove-Item -LiteralPath $a -Recurse -Force }
    }
    if (git diff --cached --name-only) {
        [System.IO.File]::WriteAllText($mensajeTmp, $Grupos[$g].M + "`n", $utf8)
        git commit -q -F $mensajeTmp
        if ($LASTEXITCODE -ne 0) { throw "No se pudo hacer el commit." }
        $hechos++
    } else {
        Write-Host "    sin cambios: ya estaba igual"
    }
}
if (Test-Path $mensajeTmp) { Remove-Item $mensajeTmp }

Write-Host ""
if ($Simular) {
    Write-Host "Simulación: no se cambió nada."
} else {
    Write-Host "$hechos commits creados. Revísalos con: git log --oneline"
    Write-Host "Si corriges algo, haz un commit normal. Después sube con git push (ver docs/division-del-trabajo.md)."
}
