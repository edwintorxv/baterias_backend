package com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado;

import com.riesgopsicosocial.application.port.in.resultado.CalcularResultadosUseCase;
import com.riesgopsicosocial.application.port.in.resultado.ConsultarResultadosUseCase;
import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoAplicacionResponse;
import com.riesgopsicosocial.shared.response.ApiResponse;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<ApiResponse<ResultadoAplicacionResponse>> calcular(@PathVariable Long idAplicacion) {
        return ResponseEntity.ok(
                ResponseBuilder.success(
                        "Resultados calculados exitosamente",
                        ResultadoResponseMapper.toResponse(calcularResultados.calcular(idAplicacion)))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ResultadoAplicacionResponse>> consultar(@PathVariable Long idAplicacion) {
        ResultadoAplicacion resultado = consultarResultados.consultar(idAplicacion);
        String mensaje = resultado.cuestionarios().isEmpty()
                ? "La aplicación aún no tiene resultados calculados"
                : "Resultados de la aplicación";
        return ResponseEntity.ok(ResponseBuilder.success(mensaje, ResultadoResponseMapper.toResponse(resultado)));
    }

}
