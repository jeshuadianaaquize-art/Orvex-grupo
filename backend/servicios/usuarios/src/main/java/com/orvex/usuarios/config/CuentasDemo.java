package com.orvex.usuarios.config;

import com.orvex.seguridad.Rol;
import com.orvex.usuarios.modelo.Repositorios.RolRepository;
import com.orvex.usuarios.modelo.Repositorios.UsuarioRepository;
import com.orvex.usuarios.modelo.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cuentas de DEMOSTRACIÓN, una por rol, para probar el sistema. */
@Component
@ConditionalOnProperty(name = "orvex.demo.habilitado", havingValue = "true")
public class CuentasDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CuentasDemo.class);

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder hash;
    private final OrvexProps props;

    public CuentasDemo(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder hash, OrvexProps props) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.hash = hash;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int creadas = 0;
        for (Rol rol : Rol.values()) {
            String correo = rol.nombreBd() + "@orvex.demo";
            if (usuarios.existsByCorreoIgnoreCase(correo)) {
                continue;
            }
            String nombre = rol.nombreBd().substring(0, 1).toUpperCase() + rol.nombreBd().substring(1) + " (demo)";
            usuarios.save(new Usuario(roles.findByNombre(rol.nombreBd()).orElseThrow(), nombre, correo,
                    hash.encode(props.demo().contrasena()), null));
            creadas++;
        }
        log.warn("MODO DEMO activo: cuentas *@orvex.demo disponibles ({} nuevas). No usar en producción.", creadas);
    }
}
