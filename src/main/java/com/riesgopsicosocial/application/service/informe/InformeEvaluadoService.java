package com.riesgopsicosocial.application.service.informe;

import com.riesgopsicosocial.application.port.in.informe.ConsultarInformeEvaluadoUseCase;
import com.riesgopsicosocial.application.port.out.informe.InformeEvaluadoPort;
import com.riesgopsicosocial.application.port.out.resultado.ResultadoPort;
import com.riesgopsicosocial.domain.model.informe.*;
import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class InformeEvaluadoService implements ConsultarInformeEvaluadoUseCase {

    private final InformeEvaluadoPort informeEvaluadoPort;
    private final ResultadoPort resultadoPort;

    public InformeEvaluadoService(InformeEvaluadoPort informeEvaluadoPort, ResultadoPort resultadoPort) {
        this.informeEvaluadoPort = informeEvaluadoPort;
        this.resultadoPort = resultadoPort;
    }

    @Override
    @Transactional(readOnly = true)
    public InformeEvaluado consultar(Long idCliente, Long idEvaluado, Integer anio) {
        DatosCliente cliente = informeEvaluadoPort.buscarCliente(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente));
        DatosEvaluado evaluado = informeEvaluadoPort.buscarEvaluado(idEvaluado)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluado no encontrado con id: " + idEvaluado));
        validarRelacion(idCliente, idEvaluado);

        List<DatosAplicacion> delPeriodo = informeEvaluadoPort.buscarAplicaciones(idCliente, idEvaluado).stream()
                .filter(datos -> anio == null || datos.fechaAplicacion().getYear() == anio)
                .sorted(Comparator.comparing(DatosAplicacion::fechaAplicacion).thenComparing(DatosAplicacion::idAplicacion))
                .toList();
        if (anio != null && delPeriodo.isEmpty()) {
            throw new ResourceNotFoundException("El evaluado " + idEvaluado + " no tiene aplicaciones con el cliente "
                    + idCliente + " en el año " + anio);
        }

        // Para un solo trabajador son pocas aplicaciones: se reutiliza la consulta de resultados de cada una.
        List<AplicacionEvaluado> aplicaciones = delPeriodo.stream()
                .map(datos -> new AplicacionEvaluado(datos, buscarResultado(datos)))
                .toList();
        return new InformeEvaluado(cliente, evaluado, aplicaciones);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> consultarAnios(Long idCliente, Long idEvaluado) {
        if (informeEvaluadoPort.buscarCliente(idCliente).isEmpty()) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente);
        }
        if (informeEvaluadoPort.buscarEvaluado(idEvaluado).isEmpty()) {
            throw new ResourceNotFoundException("Evaluado no encontrado con id: " + idEvaluado);
        }
        validarRelacion(idCliente, idEvaluado);

        return informeEvaluadoPort.buscarAplicaciones(idCliente, idEvaluado).stream()
                .map(datos -> datos.fechaAplicacion().getYear())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private void validarRelacion(Long idCliente, Long idEvaluado) {
        if (!informeEvaluadoPort.existeRelacion(idCliente, idEvaluado)) {
            throw new ResourceNotFoundException("El evaluado " + idEvaluado + " no tiene relación con el cliente " + idCliente);
        }
    }

    private ResultadoAplicacion buscarResultado(DatosAplicacion datos) {
        ResultadoAplicacion resultado = resultadoPort.buscarPorAplicacion(datos.idAplicacion(), datos.idGrupoOcupacional());
        return resultado.cuestionarios().isEmpty() ? null : resultado;
    }

}
