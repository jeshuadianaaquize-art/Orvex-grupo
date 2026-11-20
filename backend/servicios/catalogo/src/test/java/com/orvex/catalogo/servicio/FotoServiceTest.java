package com.orvex.catalogo.servicio;

import static org.junit.jupiter.api.Assertions.*;

import com.orvex.common.web.ApiException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FotoServiceTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};

    @TempDir
    Path dir;

    @Test
    void guardaUnaImagenRealYLaPuedeServir() throws Exception {
        var fotos = new FotoService(dir.toString());
        String url = fotos.guardar(7L, new MockMultipartFile("archivo", "x.png", "image/png", PNG));
        assertTrue(url.startsWith(FotoService.PREFIJO_URL + "7-") && url.endsWith(".png"));
        String nombre = url.substring(FotoService.PREFIJO_URL.length());
        assertTrue(fotos.archivo(nombre).isPresent());
        fotos.borrar(url);
        assertTrue(fotos.archivo(nombre).isEmpty());
    }

    @Test
    void laExtensionSaleDelContenidoNoDelNombre() throws Exception {
        var fotos = new FotoService(dir.toString());
        assertTrue(fotos.guardar(1L, new MockMultipartFile("archivo", "foto.png", "image/png", JPG)).endsWith(".jpg"));
    }

    @Test
    void rechazaArchivosQueNoSonImagenesAunqueDiganQueLoSon() throws Exception {
        var fotos = new FotoService(dir.toString());
        var html = new MockMultipartFile("archivo", "foto.png", "image/png", "<script>alert(1)</script>".getBytes());
        var e = assertThrows(ApiException.class, () -> fotos.guardar(1L, html));
        assertEquals("Solo se aceptan imágenes JPG, PNG o WEBP.", e.getMessage());
        try (var listado = Files.list(dir)) {
            assertEquals(0, listado.count());
        }
    }

    @Test
    void rechazaArchivoVacioOGrande() throws Exception {
        var fotos = new FotoService(dir.toString());
        assertThrows(ApiException.class, () -> fotos.guardar(1L, new MockMultipartFile("archivo", new byte[0])));
        byte[] grande = new byte[(int) FotoService.TAMANO_MAXIMO + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        assertThrows(ApiException.class, () -> fotos.guardar(1L, new MockMultipartFile("archivo", "g.png", "image/png", grande)));
    }

    @Test
    void noSirveArchivosFueraDelDirectorioNiNombresInventados() throws Exception {
        var fotos = new FotoService(dir.toString());
        Files.writeString(dir.getParent().resolve("secreto.txt"), "x");
        assertTrue(fotos.archivo("../secreto.txt").isEmpty());
        assertTrue(fotos.archivo("..%2Fsecreto.txt").isEmpty());
        assertTrue(fotos.archivo("1-00000000-0000-0000-0000-000000000000.png").isEmpty()); // formato válido, no existe
    }
}
