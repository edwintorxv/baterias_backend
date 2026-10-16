package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe;

import com.riesgopsicosocial.application.port.in.informe.ConsultarInformeEmpresaUseCase;
import com.riesgopsicosocial.application.port.in.informe.GenerarDocumentoInformeEmpresaUseCase;
import com.riesgopsicosocial.domain.model.informe.DocumentoInforme;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto.*;
import com.riesgopsicosocial.shared.exception.ValidationException;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes/{idCliente}/informe")
public class InformeEmpresaController {

    private static final MediaType DOCX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final ConsultarInformeEmpresaUseCase consultarInformeEmpresa;
    private final GenerarDocumentoInformeEmpresaUseCase generarDocumentoInformeEmpresa;

    public InformeEmpresaController(ConsultarInformeEmpresaUseCase consultarInformeEmpresa,
                                    GenerarDocumentoInformeEmpresaUseCase generarDocumentoInformeEmpresa) {
        this.consultarInformeEmpresa = consultarInformeEmpresa;
        this.generarDocumentoInformeEmpresa = generarDocumentoInformeEmpresa;
    }

    /** Informe de la empresa en el año: lo mismo del Word, sin los textos fijos ni los espacios para llenar a mano. */
    @GetMapping
    public ResponseEntity<ApiResponse<InformeEmpresaResponse>> consultarInformeEmpresa(@PathVariable Long idCliente,
                                                                                      @RequestParam int anio) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Informe de la empresa",
                        InformeEmpresaResponseMapper.toResponse(consultarInformeEmpresa.consultar(idCliente, anio)))
        );
    }

    @GetMapping("/anios")
    public ResponseEntity<ApiResponse<List<Integer>>> consultarAniosInformeEmpresa(@PathVariable Long idCliente) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Años con aplicaciones del cliente",
                        consultarInformeEmpresa.consultarAnios(idCliente))
        );
    }

    /** Archivo del informe de empresa del año; por ahora solo en Word ({@code formato=docx}). */
    @GetMapping("/archivo")
    public ResponseEntity<byte[]> descargarInformeEmpresa(@PathVariable Long idCliente,
                                                          @RequestParam int anio,
                                                          @RequestParam(defaultValue = "docx") String formato) {
        FormatoDocumento formatoDocumento = FormatoDocumento.deExtension(formato)
                .orElseThrow(() -> new ValidationException("Formato de informe no válido: " + formato,
                        List.of("Formatos permitidos: pdf, docx")));
        DocumentoInforme documento = generarDocumentoInformeEmpresa.generar(idCliente, anio, formatoDocumento);
        return ResponseEntity.ok()
                .contentType(documento.formato() == FormatoDocumento.PDF ? MediaType.APPLICATION_PDF : DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(documento.nombreArchivo()).build().toString())
                .body(documento.contenido());
    }

}
