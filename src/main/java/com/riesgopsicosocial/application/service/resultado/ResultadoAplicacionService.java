package com.riesgopsicosocial.application.service.resultado;

import com.riesgopsicosocial.application.port.in.resultado.CalcularResultadosUseCase;
import com.riesgopsicosocial.application.port.in.resultado.ConsultarResultadosUseCase;
import com.riesgopsicosocial.application.port.out.resultado.DatosCalculoPort;
import com.riesgopsicosocial.application.port.out.resultado.ResultadoPort;
import com.riesgopsicosocial.domain.model.resultado.configuracion.ConfiguracionCuestionario;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import com.riesgopsicosocial.domain.model.resultado.configuracion.RespuestaCalculo;
import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.domain.model.resultado.ResultadoCuestionario;
import com.riesgopsicosocial.domain.model.resultado.ResultadoTotalGeneral;
import com.riesgopsicosocial.domain.service.resultado.CalculadoraResultado;
import com.riesgopsicosocial.domain.service.resultado.VerificadorAplicacionCompleta;
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

        // Se calculan todos los cuestionarios del grupo (A o B, C y D) y solo con la aplicación completa.
        Map<Long, ConfiguracionCuestionario> configuraciones = new TreeMap<>();
        datosCalculoPort.buscarCuestionariosDelGrupo(idGrupo)
                .forEach(id -> configuraciones.put(id, datosCalculoPort.cargarConfiguracion(id, idGrupo)));
        validarCuestionariosDelGrupo(idAplicacion, respuestas, configuraciones.keySet());
        validarUnSoloCuestionarioPorTipo(idAplicacion, configuraciones.values());
        VerificadorAplicacionCompleta.exigirCompleta(configuraciones.values(), respuestas, filtros);

        Map<Long, List<RespuestaCalculo>> porCuestionario = respuestas.stream()
                .collect(Collectors.groupingBy(RespuestaCalculo::idCuestionario));
        Map<Long, ResultadoCuestionario> cuestionarios = new TreeMap<>();
        configuraciones.forEach((idCuestionario, config) -> cuestionarios.put(idCuestionario,
                calculadora.calcular(config, porCuestionario.getOrDefault(idCuestionario, List.of()), filtros)));

        ResultadoAplicacion resultado = new ResultadoAplicacion(idAplicacion, idGrupo, LocalDateTime.now(),
                List.copyOf(cuestionarios.values()), calcularTotalGeneral(configuraciones, cuestionarios));
        resultadoPort.reemplazar(resultado);
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoAplicacion consultar(Long idAplicacion) {
        Long idGrupo = obtenerGrupo(idAplicacion);
        return resultadoPort.buscarPorAplicacion(idAplicacion, idGrupo);
    }

    /** Respuestas de un cuestionario que no es del grupo (p. ej. quedaron de antes de cambiar el grupo): no se calcula. */
    private void validarCuestionariosDelGrupo(Long idAplicacion, List<RespuestaCalculo> respuestas, Set<Long> delGrupo) {
        if (delGrupo.isEmpty()) {
            throw new BusinessException("El grupo ocupacional de la aplicación " + idAplicacion + " no tiene cuestionarios configurados");
        }
        Set<Long> ajenos = respuestas.stream()
                .map(RespuestaCalculo::idCuestionario)
                .filter(id -> !delGrupo.contains(id))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!ajenos.isEmpty()) {
            throw new BusinessException("La aplicación " + idAplicacion + " tiene respuestas de cuestionarios que no son de su"
                    + " grupo ocupacional (ids " + ajenos + ")");
        }
    }

    /**
     * Una aplicación lleva una sola forma intralaboral (A o B, según el cargo) y un solo
     * extralaboral; con dos no se sabría qué baremo usar para el total general.
     */
    private void validarUnSoloCuestionarioPorTipo(Long idAplicacion, Collection<ConfiguracionCuestionario> configuraciones) {
        for (MetodoCalculo metodo : List.of(MetodoCalculo.SUMA_POR_DOMINIOS, MetodoCalculo.SUMA_DIRECTA)) {
            List<String> formas = configuraciones.stream()
                    .filter(c -> c.metodoCalculo() == metodo)
                    .map(ConfiguracionCuestionario::forma)
                    .toList();
            if (formas.size() > 1) {
                throw new BusinessException("La aplicación " + idAplicacion + " tiene respuestas de los cuestionarios "
                        + String.join(" y ", formas) + "; solo puede tener uno de tipo " + metodo);
            }
        }
    }

    /** Intralaboral + extralaboral; {@code null} si la aplicación no tiene ambos. */
    private ResultadoTotalGeneral calcularTotalGeneral(Map<Long, ConfiguracionCuestionario> configuraciones,
                                                       Map<Long, ResultadoCuestionario> cuestionarios) {
        Optional<ConfiguracionCuestionario> intralaboral = buscarPorMetodo(configuraciones, MetodoCalculo.SUMA_POR_DOMINIOS);
        Optional<ConfiguracionCuestionario> extralaboral = buscarPorMetodo(configuraciones, MetodoCalculo.SUMA_DIRECTA);
        if (intralaboral.isEmpty() || extralaboral.isEmpty()) {
            return null;
        }
        ConfiguracionCuestionario intra = intralaboral.get();
        ConfiguracionCuestionario extra = extralaboral.get();
        return calculadora.calcularTotalGeneral(
                intra, cuestionarios.get(intra.idCuestionario()),
                extra, cuestionarios.get(extra.idCuestionario()),
                datosCalculoPort.cargarBaremosTotalGeneral(intra.idCuestionario()));
    }

    private Optional<ConfiguracionCuestionario> buscarPorMetodo(Map<Long, ConfiguracionCuestionario> configuraciones,
                                                                MetodoCalculo metodo) {
        return configuraciones.values().stream().filter(c -> c.metodoCalculo() == metodo).findFirst();
    }

    private Long obtenerGrupo(Long idAplicacion) {
        return datosCalculoPort.buscarGrupoOcupacional(idAplicacion)
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación no encontrada con id: " + idAplicacion));
    }

}
