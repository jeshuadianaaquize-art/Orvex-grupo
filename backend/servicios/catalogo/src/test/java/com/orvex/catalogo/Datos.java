package com.orvex.catalogo;

import com.orvex.catalogo.modelo.CategoriaEntidad;
import com.orvex.catalogo.modelo.EstadoProducto;
import com.orvex.catalogo.modelo.Producto;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;

/** Productos en memoria para las pruebas (sin base de datos). */
public final class Datos {

    private Datos() {
    }

    public static CategoriaEntidad categoria(Categoria c) {
        try {
            Constructor<CategoriaEntidad> ctor = CategoriaEntidad.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            CategoriaEntidad e = ctor.newInstance();
            set(CategoriaEntidad.class, e, "id", (short) (c.ordinal() + 1));
            set(CategoriaEntidad.class, e, "nombre", c.nombreBd());
            return e;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Producto producto(long id, Categoria c, String nombre, String marca, String precio,
                                    EstadoProducto estado, Map<String, String> specs) {
        Producto p = new Producto(categoria(c), "SKU-" + id, nombre, marca, new BigDecimal(precio));
        p.reemplazarEspecificaciones(specs);
        p.cambiarEstado(estado);
        try {
            set(Producto.class, p, "id", id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return p;
    }

    private static void set(Class<?> tipo, Object o, String campo, Object valor) throws ReflectiveOperationException {
        Field f = tipo.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(o, valor);
    }
}
