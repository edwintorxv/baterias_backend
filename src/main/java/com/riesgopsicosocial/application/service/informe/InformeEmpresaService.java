package com.riesgopsicosocial.application.service.informe;

import com.riesgopsicosocial.application.port.in.informe.ConsultarInformeEmpresaUseCase;
import com.riesgopsicosocial.application.port.out.informe.InformeEmpresaPort;
import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.empresa.*;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.service.informe.AnalizadorInformeEmpresa;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InformeEmpresaService implements ConsultarInformeEmpresaUseCase {

    private final InformeEmpresaPort informeEmpresaPort;

    public InformeEmpresaService(InformeEmpresaPort informeEmpresaPort) {
        this.informeEmpresaPort = informeEmpresaPort;
    }

    @Override
    @Transactional(readOnly = true)
    public InformeEmpresa consultar(Long idCliente, int anio) {
        DatosCliente cliente = buscarCliente(idCliente);
        List<PersonaCargada> personas = informeEmpresaPort.buscarPersonasCargadas(idCliente);
        if (personas.isEmpty()) {
            throw new BusinessException("El cliente " + idCliente + " no tiene personas cargadas para evaluar");
        }
        List<AplicacionEmpresa> aplicaciones = informeEmpresaPort.buscarAplicaciones(idCliente, anio);
        if (aplicaciones.isEmpty()) {
            throw new ResourceNotFoundException("El cliente " + idCliente + " no tiene aplicaciones en el año " + anio);
        }

        Map<Long, List<AplicacionEmpresa>> porPersona = aplicaciones.stream()
                .collect(Collectors.groupingBy(AplicacionEmpresa::idEvaluadoCliente));
        List<ParticipanteEmpresa> participantes = personas.stream()
                .map(p -> participante(p, porPersona.getOrDefault(p.idEvaluadoCliente(), List.of())))
                .toList();

        AnalizadorInformeEmpresa analizador = new AnalizadorInformeEmpresa(
                informeEmpresaPort.buscarCuestionarios(), informeEmpresaPort.buscarNiveles());
        return new InformeEmpresa(cliente, anio, analizador.analizar(participantes));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> consultarAnios(Long idCliente) {
        buscarCliente(idCliente);
        return informeEmpresaPort.buscarAnios(idCliente).stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private DatosCliente buscarCliente(Long idCliente) {
        return informeEmpresaPort.buscarCliente(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente));
    }

    /**
     * Evaluada si respondió en alguna aplicación del año; si tiene varias con resultados,
     * cuenta la más reciente.
     */
    private static ParticipanteEmpresa participante(PersonaCargada persona, List<AplicacionEmpresa> aplicaciones) {
        boolean tieneRespuestas = aplicaciones.stream().anyMatch(AplicacionEmpresa::tieneRespuestas);
        Optional<AplicacionEmpresa> calculada = aplicaciones.stream()
                .filter(a -> a.resultado() != null)
                .max(Comparator.comparing(AplicacionEmpresa::fechaAplicacion).thenComparing(AplicacionEmpresa::idAplicacion));
        return new ParticipanteEmpresa(persona.idEvaluadoCliente(), persona.area(), persona.ciudad(), tieneRespuestas,
                calculada.map(AplicacionEmpresa::resultado).orElse(null),
                calculada.map(AplicacionEmpresa::filtros).orElse(FiltrosAplicacion.SIN_REGISTRO));
    }

}
