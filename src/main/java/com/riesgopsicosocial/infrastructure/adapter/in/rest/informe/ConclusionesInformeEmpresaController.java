package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto.ConclusionesInformeRequest;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto.ConclusionesInformeResponse;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.informeempresa.ConclusionesInformeEmpresaService;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.informeempresa.InformeEmpresaEntity;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Conclusiones y plan de acción del informe de empresa (CRUD sin cálculo: va directo al servicio de
 * persistencia, como los demás catálogos y entidades planas).
 */
@RestController
@RequestMapping("/clientes/{idCliente}/informe/conclusiones")
public class ConclusionesInformeEmpresaController {

    private final ConclusionesInformeEmpresaService service;

    public ConclusionesInformeEmpresaController(ConclusionesInformeEmpresaService service) {
        this.service = service;
    }

    private static ConclusionesInformeResponse toResponse(InformeEmpresaEntity informe) {
        return new ConclusionesInformeResponse(informe.getFkCliente(), informe.getAnio(), informe.getId() != null,
                informe.getConclusiones(), informe.getAccionesCortoPlazo(), informe.getAccionesMedianoPlazo(),
                informe.getAccionesLargoPlazo(), informe.getFkEvaluador(), informe.getFechaCreado(),
                informe.getFechaModificado());
    }

    /** Lo guardado del año; si aún no hay nada, todo vacío con {@code guardado = false}. */
    @GetMapping
    public ResponseEntity<ApiResponse<ConclusionesInformeResponse>> consultar(@PathVariable Long idCliente,
                                                                             @RequestParam int anio) {
        return ResponseEntity.ok(ResponseBuilder.success("Conclusiones del informe de empresa",
                toResponse(service.consultar(idCliente, anio))));
    }

    /** Crea o reemplaza las conclusiones, el plan de acción y el profesional del año. */
    @PutMapping
    public ResponseEntity<ApiResponse<ConclusionesInformeResponse>> guardar(@PathVariable Long idCliente,
                                                                           @RequestParam int anio,
                                                                           @RequestBody ConclusionesInformeRequest request) {
        InformeEmpresaEntity datos = new InformeEmpresaEntity();
        datos.setConclusiones(request.conclusiones());
        datos.setAccionesCortoPlazo(request.accionesCortoPlazo());
        datos.setAccionesMedianoPlazo(request.accionesMedianoPlazo());
        datos.setAccionesLargoPlazo(request.accionesLargoPlazo());
        datos.setFkEvaluador(request.fkEvaluador());
        return ResponseEntity.ok(ResponseBuilder.success("Conclusiones guardadas exitosamente",
                toResponse(service.guardar(idCliente, anio, datos))));
    }

}
