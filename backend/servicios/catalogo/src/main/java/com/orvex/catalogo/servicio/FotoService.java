package com.orvex.catalogo.servicio;

import com.orvex.common.web.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Fotos de producto (RF-03: "fotos") */
@Service
public class FotoService {

    public static final long TAMANO_MAXIMO = 2L * 1024 * 1024;
    public static final String PREFIJO_URL = "/api/catalogo/fotos/";

    private final Path directorio;

    public FotoService(@Value("${orvex.fotos.directorio}") String directorio) throws IOException {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
        Files.createDirectories(this.directorio);
    }

    /** Guarda la imagen y devuelve la URL pública que va en producto.foto_url. */
    public String guardar(Long productoId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Elige una imagen.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen pesa más de 2 MB.");
        }
        try {
            byte[] cabecera;
            try (InputStream in = archivo.getInputStream()) {
                cabecera = in.readNBytes(12);
            }
            String ext = extension(cabecera).orElseThrow(() ->
                    new ApiException(HttpStatus.BAD_REQUEST, "Solo se aceptan imágenes JPG, PNG o WEBP."));
            String nombre = productoId + "-" + UUID.randomUUID() + "." + ext;
            Path destino = directorio.resolve(nombre).normalize();
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino);
            }
            return PREFIJO_URL + nombre;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen.");
        }
    }

    /** Ruta del archivo, solo si el nombre es uno de los que genera este servicio */
    public Optional<Path> archivo(String nombre) {
        if (nombre == null || !nombre.matches("^\\d+-[0-9a-f-]{36}\\.(jpg|png|webp)$")) {
            return Optional.empty();
        }
        Path p = directorio.resolve(nombre).normalize();
        return p.startsWith(directorio) && Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
    }

    /** Borra la foto anterior al reemplazarla. Ignora URLs que no son de este servicio. */
    public void borrar(String url) {
        if (url == null || !url.startsWith(PREFIJO_URL)) {
            return;
        }
        archivo(url.substring(PREFIJO_URL.length())).ifPresent(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignorada) {
                // Si no se puede borrar, solo queda un archivo huérfano; no afecta al producto.
            }
        });
    }

    static Optional<String> extension(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("jpg");
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of("png");
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of("webp");
        }
        return Optional.empty();
    }
}
