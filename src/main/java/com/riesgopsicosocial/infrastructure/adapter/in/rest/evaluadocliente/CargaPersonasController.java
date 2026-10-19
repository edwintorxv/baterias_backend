package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente;

import com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas.CargaPersonasService;
import com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas.ResultadoCarga;
import com.riesgopsicosocial.shared.exception.ValidationException;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Carga de personas por archivo (solo el psicólogo; cliente ajeno → 404 por AlcanceClienteInterceptor):
 * plantilla vacía, validación (no guarda) y confirmación (todo o nada).
 */
@RestController
@RequestMapping("/clientes/{idCliente}/personas")
public class CargaPersonasController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final CargaPersonasService service;

    public CargaPersonasController(CargaPersonasService service) {
        this.service = service;
    }

    /** Siempre vacía (nunca trae las personas ya cargadas). */
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> descargarPlantilla(@PathVariable Long idCliente) {
        CargaPersonasService.Plantilla plantilla = service.plantilla(idCliente);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(plantilla.nombreArchivo()).build().toString())
                .body(plantilla.contenido());
    }

    /**
     * Sin {@code confirmar}: dice qué pasará con cada fila, sin guardar. Con {@code confirmar=true}: guarda
     * todas las filas, o ninguna si alguna tiene errores (409 con las filas en {@code details}).
     */
    @PostMapping(value = "/carga", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ResultadoCarga>> cargar(@PathVariable Long idCliente,
                                                              @RequestParam("archivo") MultipartFile archivo,
                                                              @RequestParam(defaultValue = "false") boolean confirmar) {
        byte[] contenido = leer(archivo);
        if (!confirmar) {
            ResultadoCarga resultado = service.validar(idCliente, contenido);
            String mensaje = resultado.puedeConfirmar()
                    ? "Archivo válido: " + resultado.totalFilas() + " persona(s) listas para guardar"
                    : "El archivo tiene " + resultado.resumen().get("ERROR") + " fila(s) con errores";
            return ResponseEntity.ok(ResponseBuilder.success(mensaje, resultado));
        }
        ResultadoCarga resultado = service.confirmar(idCliente, contenido);
        return ResponseEntity.ok(ResponseBuilder.success(
                "Se guardaron " + resultado.totalFilas() + " persona(s) del archivo", resultado));
    }

    private static byte[] leer(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase();
        if (archivo.isEmpty() || !nombre.endsWith(".xlsx")) {
            throw new ValidationException("Suba la plantilla de personas en Excel (.xlsx)",
                    List.of("Archivo recibido: " + (nombre.isEmpty() ? "ninguno" : archivo.getOriginalFilename())));
        }
        if (archivo.getSize() > CargaPersonasService.MAXIMO_BYTES) {
            throw new ValidationException("El archivo pesa más de 5 MB", List.of("Divídalo en varios archivos"));
        }
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new ValidationException("No se pudo leer el archivo", List.of(e.getMessage()));
        }
    }

}
