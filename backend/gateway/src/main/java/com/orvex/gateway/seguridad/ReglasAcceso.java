package com.orvex.gateway.seguridad;

import com.orvex.seguridad.Rol;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

/** Quién puede llamar a cada ruta (RNF-04: control de acceso por rol en el gateway). */
public final class ReglasAcceso {

    public sealed interface Requisito permits Publico, Sesion, Roles {
    }

    public record Publico() implements Requisito {
    }

    public record Sesion() implements Requisito {
    }

    public record Roles(Set<Rol> permitidos) implements Requisito {
    }

    private record Regla(HttpMethod metodo, String patron, Requisito requisito) {
    }

    private static final Requisito PUBLICO = new Publico();
    private static final Requisito SESION = new Sesion();
    private static final Requisito SOLO_ADMIN = new Roles(Set.of(Rol.ADMINISTRADOR));
    private static final Requisito ALMACEN = new Roles(Set.of(Rol.ALMACENERO, Rol.ADMINISTRADOR));
    private static final Requisito CAJA = new Roles(Set.of(Rol.VENDEDOR, Rol.ADMINISTRADOR));

    /** La primera regla que coincide decide. null en el método = cualquier método. */
    private static final List<Regla> REGLAS = List.of(
            // Catálogo y armador: el visitante navega sin sesión (HU-04, HU-06).
            new Regla(HttpMethod.GET, "/api/productos/**", PUBLICO),
            new Regla(HttpMethod.GET, "/api/categorias/**", PUBLICO),
            new Regla(HttpMethod.GET, "/api/catalogo/fotos/**", PUBLICO),
            // Administración del catálogo (HU-05): cualquier método.
            new Regla(null, "/api/catalogo/admin/**", SOLO_ADMIN),
            new Regla(HttpMethod.POST, "/api/armador/verificar", PUBLICO),
            // Optimización en vivo del armado (HU-09): también para visitantes; las guardadas exigen sesión.
            new Regla(HttpMethod.POST, "/api/optimizacion/analizar", PUBLICO),
            // Cuenta (HU-01, HU-02, HU-03).
            new Regla(HttpMethod.POST, "/api/auth/registro", PUBLICO),
            new Regla(HttpMethod.POST, "/api/auth/login", PUBLICO),
            new Regla(HttpMethod.POST, "/api/auth/recuperar", PUBLICO),
            new Regla(HttpMethod.POST, "/api/auth/restablecer", PUBLICO),
            new Regla(HttpMethod.POST, "/api/auth/logout", SESION),
            // Perfil propio: cualquier usuario con sesión.
            new Regla(null, "/api/usuarios/me", SESION),
            new Regla(null, "/api/usuarios/me/**", SESION),
            // Pago QR: el banco confirma por webhook desde afuera, sin sesión; lo protege la firma HMAC (RNF-14).
            new Regla(HttpMethod.POST, "/api/pagos/webhook", PUBLICO),
            // Caja de la tienda física (HU-16): vendedor y administrador.
            new Regla(null, "/api/pos/**", CAJA),
            // Todas las ventas y su anulación (HU-20, RF-26).
            new Regla(null, "/api/pedidos/admin", SOLO_ADMIN),
            new Regla(null, "/api/pedidos/admin/**", SOLO_ADMIN),
            // Preparación y entrega de pedidos (almacenero). El resto de /api/carrito y /api/pedidos exige sesión.
            new Regla(null, "/api/pedidos/gestion", ALMACEN),
            new Regla(null, "/api/pedidos/gestion/**", ALMACEN),
            // Inventario y proveedores (HU-11, HU-13, HU-14).
            new Regla(null, "/api/inventario/**", ALMACEN),
            new Regla(null, "/api/proveedores/**", ALMACEN),
            new Regla(null, "/api/proveedores", ALMACEN),
            // Administración (HU-19, HU-20, HU-21, HU-25).
            new Regla(null, "/api/usuarios/**", SOLO_ADMIN),
            new Regla(null, "/api/usuarios", SOLO_ADMIN),
            new Regla(null, "/api/roles/**", SOLO_ADMIN),
            new Regla(null, "/api/estado", SOLO_ADMIN),
            new Regla(null, "/api/reglas", SOLO_ADMIN),
            new Regla(null, "/api/reglas/**", SOLO_ADMIN),
            new Regla(null, "/api/auditoria/**", SOLO_ADMIN),
            // Investigación operativa: la política de reposición de un producto también es del almacenero.
            new Regla(HttpMethod.GET, "/api/reportes/markov/inventario/**", ALMACEN),
            new Regla(null, "/api/reportes/**", SOLO_ADMIN));

    private static final AntPathMatcher RUTAS = new AntPathMatcher();

    private ReglasAcceso() {
    }

    public static Requisito para(HttpMethod metodo, String ruta) {
        if (HttpMethod.OPTIONS.equals(metodo)) {
            return PUBLICO; // preflight CORS: el navegador no envía el token
        }
        for (Regla r : REGLAS) {
            if ((r.metodo() == null || r.metodo().equals(metodo)) && RUTAS.match(r.patron(), ruta)) {
                return r.requisito();
            }
        }
        return SESION;
    }
}
