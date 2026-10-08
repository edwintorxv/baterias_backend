package com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado;

import com.riesgopsicosocial.application.port.in.resultado.CalcularResultadosUseCase;
import com.riesgopsicosocial.application.port.in.resultado.ConsultarResultadosUseCase;
import com.riesgopsicosocial.domain.model.resultado.*;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoAplicacionResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoCuestionarioResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoDetalleResponse;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoTotalGeneralResponse;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/aplicaciones/{idAplicacion}/resultados")
public class ResultadoController {

    private final CalcularResultadosUseCase calcularResultados;
    private final ConsultarResultadosUseCase consultarResultados;

    public ResultadoController(CalcularResultadosUseCase calcularResultados,
                               ConsultarResultadosUseCase consultarResultados) {
        this.calcularResultados = calcularResultados;
        this.consultarResultados = consultarResultados;
    }

    private ResultadoAplicacionResponse toResponse(ResultadoAplicacion resultado) {
        return new ResultadoAplicacionResponse(
                resultado.idAplicacion(),
                resultado.idGrupoOcupacional(),
                resultado.fechaCalculo(),
                resultado.cuestionarios().stream().map(this::toResponse).toList(),
                toResponse(resultado.totalGeneral())
        );
    }

    private ResultadoTotalGeneralResponse toResponse(ResultadoTotalGeneral total) {
        if (total == null) {
            return null;
        }
        return new ResultadoTotalGeneralResponse(
                total.idCuestionarioIntralaboral(),
                total.formaIntralaboral(),
                total.puntajeBruto(),
                total.puntajeTransformado(),
                total.nivelRiesgo().id(),
                total.nivelRiesgo().nombre());
    }

    private ResultadoCuestionarioResponse toResponse(ResultadoCuestionario rc) {
        return new ResultadoCuestionarioResponse(
                rc.idCuestionario(),
                rc.forma(),
                rc.puntajeBruto(),
                rc.puntajeTransformado(),
                rc.nivelRiesgo().id(),
                rc.nivelRiesgo().nombre(),
                rc.dominios().stream()
                        .map(d -> detalle(d.idDominioCuestionario(), d.nombre(), d.puntajeBruto(),
                                d.puntajeTransformado(), d.nivelRiesgo()))
                        .toList(),
                rc.dimensiones().stream()
                        .map(d -> detalle(d.idDimensionCuestionario(), d.nombre(), d.puntajeBruto(),
                                d.puntajeTransformado(), d.nivelRiesgo()))
                        .toList()
        );
    }

    private ResultadoDetalleResponse detalle(Long id, String nombre, BigDecimal bruto,
                                             BigDecimal transformado, NivelRiesgo nivel) {
        return new ResultadoDetalleResponse(id, nombre, bruto, transformado, nivel.id(), nivel.nombre());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResultadoAplicacionResponse>> calcular(@PathVariable Long idAplicacion) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Resultados calculados exitosamente",
                        toResponse(calcularResultados.calcular(idAplicacion)))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultadoAplicacionResponse>> consultar(@PathVariable Long idAplicacion) {
        ResultadoAplicacion resultado = consultarResultados.consultar(idAplicacion);
        String mensaje = resultado.cuestionarios().isEmpty()
                ? "La aplicación aún no tiene resultados calculados"
                : "Resultados de la aplicación";
        return ResponseEntity.ok(ResponseBuilder.success(mensaje, toResponse(resultado)));
    }

}
