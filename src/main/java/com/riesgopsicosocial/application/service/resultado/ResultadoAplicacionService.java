package com.riesgopsicosocial.application.service.resultado;

import com.riesgopsicosocial.application.port.in.resultado.CalcularResultadosUseCase;
import com.riesgopsicosocial.application.port.in.resultado.ConsultarResultadosUseCase;
import com.riesgopsicosocial.application.port.out.resultado.DatosCalculoPort;
import com.riesgopsicosocial.application.port.out.resultado.ResultadoPort;
import com.riesgopsicosocial.domain.model.resultado.configuracion.ConfiguracionCuestionario;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.RespuestaCalculo;
import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.domain.model.resultado.ResultadoCuestionario;
import com.riesgopsicosocial.domain.service.resultado.CalculadoraResultado;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResultadoAplicacionService implements CalcularResultadosUseCase, ConsultarResultadosUseCase {

    private final DatosCalculoPort datosCalculoPort;
    private final ResultadoPort resultadoPort;
    private final CalculadoraResultado calculadora = new CalculadoraResultado();

    public ResultadoAplicacionService(DatosCalculoPort datosCalculoPort, ResultadoPort resultadoPort) {
        this.datosCalculoPort = datosCalculoPort;
        this.resultadoPort = resultadoPort;
    }

    @Override
    @Transactional
    public ResultadoAplicacion calcular(Long idAplicacion) {
        Long idGrupo = obtenerGrupo(idAplicacion);

        List<RespuestaCalculo> respuestas = datosCalculoPort.buscarRespuestas(idAplicacion);
        if (respuestas.isEmpty()) {
            throw new BusinessException("La aplicación " + idAplicacion + " no tiene respuestas registradas");
        }
        FiltrosAplicacion filtros = datosCalculoPort.buscarFiltros(idAplicacion);

        // Solo se calculan los cuestionarios que tienen al menos una respuesta (ej. A + C + D).
        Map<Long, List<RespuestaCalculo>> porCuestionario = respuestas.stream()
                .collect(Collectors.groupingBy(RespuestaCalculo::idCuestionario, TreeMap::new, Collectors.toList()));

        List<ResultadoCuestionario> cuestionarios = new ArrayList<>();
        porCuestionario.forEach((idCuestionario, suyas) -> {
            ConfiguracionCuestionario config = datosCalculoPort.cargarConfiguracion(idCuestionario, idGrupo);
            cuestionarios.add(calculadora.calcular(config, suyas, filtros));
        });

        ResultadoAplicacion resultado = new ResultadoAplicacion(idAplicacion, idGrupo, LocalDateTime.now(), cuestionarios);
        resultadoPort.reemplazar(resultado);
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoAplicacion consultar(Long idAplicacion) {
        Long idGrupo = obtenerGrupo(idAplicacion);
        return resultadoPort.buscarPorAplicacion(idAplicacion, idGrupo);
    }

    private Long obtenerGrupo(Long idAplicacion) {
        return datosCalculoPort.buscarGrupoOcupacional(idAplicacion)
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación no encontrada con id: " + idAplicacion));
    }

}
