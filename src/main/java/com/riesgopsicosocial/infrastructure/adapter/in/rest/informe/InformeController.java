package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe;

import com.riesgopsicosocial.application.port.in.informe.ConsultarInformeEvaluadoUseCase;
import com.riesgopsicosocial.domain.model.informe.AplicacionEvaluado;
import com.riesgopsicosocial.domain.model.informe.DatosAplicacion;
import com.riesgopsicosocial.domain.model.informe.InformeEvaluado;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto.AplicacionInformeResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto.ClienteInformeResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto.EvaluadoInformeResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto.InformeEvaluadoResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.ResultadoResponseMapper;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes/{idCliente}")
public class InformeController {

    private final ConsultarInformeEvaluadoUseCase consultarInformeEvaluado;

    public InformeController(ConsultarInformeEvaluadoUseCase consultarInformeEvaluado) {
        this.consultarInformeEvaluado = consultarInformeEvaluado;
    }

    private InformeEvaluadoResponse toResponse(InformeEvaluado informe) {
        return new InformeEvaluadoResponse(
                new ClienteInformeResponse(informe.cliente().id(), informe.cliente().nit(), informe.cliente().nombre()),
                new EvaluadoInformeResponse(informe.evaluado().id(), informe.evaluado().numeroIdentificacion(),
                        informe.evaluado().nombre(), informe.evaluado().apellido()),
                informe.aplicaciones().stream().map(this::toResponse).toList()
        );
    }

    private AplicacionInformeResponse toResponse(AplicacionEvaluado aplicacion) {
        DatosAplicacion datos = aplicacion.datos();
        return new AplicacionInformeResponse(
                datos.idAplicacion(),
                datos.fechaAplicacion(),
                datos.nombreCargo(),
                datos.nombreArea(),
                datos.idGrupoOcupacional(),
                datos.grupoOcupacional(),
                aplicacion.tieneResultados() ? "CALCULADO" : "PENDIENTE",
                aplicacion.tieneResultados() ? ResultadoResponseMapper.toResponse(aplicacion.resultado()) : null
        );
    }

    /** Sin {@code anio}: historial completo con el cliente; con {@code anio}: solo las aplicaciones de ese año. */
    @GetMapping("/evaluados/{idEvaluado}/informe")
    public ResponseEntity<ApiResponse<InformeEvaluadoResponse>> consultarInformeEvaluado(@PathVariable Long idCliente,
                                                                                        @PathVariable Long idEvaluado,
                                                                                        @RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Informe del evaluado",
                        toResponse(consultarInformeEvaluado.consultar(idCliente, idEvaluado, anio)))
        );
    }

    @GetMapping("/evaluados/{idEvaluado}/informe/anios")
    public ResponseEntity<ApiResponse<List<Integer>>> consultarAniosInformeEvaluado(@PathVariable Long idCliente,
                                                                                   @PathVariable Long idEvaluado) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Años con aplicaciones del evaluado",
                        consultarInformeEvaluado.consultarAnios(idCliente, idEvaluado))
        );
    }

}
