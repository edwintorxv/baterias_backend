package com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta;

import com.riesgopsicosocial.domain.model.resultado.configuracion.CondicionAplicacion;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario.BaremoCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario.BaremoCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle.EscalaDetalleJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultado.BorradoResultados;
import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import com.riesgopsicosocial.shared.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Digitación de una aplicación: el formulario de sus cuestionarios y el guardado de las respuestas de un
 * cuestionario en bloque (todo o nada). Guardar reemplaza las respuestas de ese cuestionario; si cambian y la
 * aplicación tenía resultados, se borran (queda pendiente de cálculo).
 */
@Service
public class RespuestasAplicacionService {

    /** Respuesta digitada: la pregunta y la opción elegida. */
    public record RespuestaNueva(Long fkPregunta, Long fkOpcionRespuesta) {
    }

    /** @param resultadosBorrados la aplicación tenía resultados y se borraron: hay que volver a calcular */
    public record Guardado(FormularioAplicacion formulario, boolean huboCambios, boolean resultadosBorrados) {
    }

    private final AplicacionJpaRepository aplicacionRepository;
    private final RespuestaJpaRepository respuestaRepository;
    private final CuestionarioJpaRepository cuestionarioRepository;
    private final BaremoCuestionarioJpaRepository baremoCuestionarioRepository;
    private final DimensionCuestionarioJpaRepository dimensionCuestionarioRepository;
    private final PreguntaJpaRepository preguntaRepository;
    private final EscalaDetalleJpaRepository escalaDetalleRepository;
    private final OpcionRespuestaJpaRepository opcionRespuestaRepository;
    private final BorradoResultados borradoResultados;

    public RespuestasAplicacionService(AplicacionJpaRepository aplicacionRepository,
                                       RespuestaJpaRepository respuestaRepository,
                                       CuestionarioJpaRepository cuestionarioRepository,
                                       BaremoCuestionarioJpaRepository baremoCuestionarioRepository,
                                       DimensionCuestionarioJpaRepository dimensionCuestionarioRepository,
                                       PreguntaJpaRepository preguntaRepository,
                                       EscalaDetalleJpaRepository escalaDetalleRepository,
                                       OpcionRespuestaJpaRepository opcionRespuestaRepository,
                                       BorradoResultados borradoResultados) {
        this.aplicacionRepository = aplicacionRepository;
        this.respuestaRepository = respuestaRepository;
        this.cuestionarioRepository = cuestionarioRepository;
        this.baremoCuestionarioRepository = baremoCuestionarioRepository;
        this.dimensionCuestionarioRepository = dimensionCuestionarioRepository;
        this.preguntaRepository = preguntaRepository;
        this.escalaDetalleRepository = escalaDetalleRepository;
        this.opcionRespuestaRepository = opcionRespuestaRepository;
        this.borradoResultados = borradoResultados;
    }

    @Transactional(readOnly = true)
    public FormularioAplicacion formulario(Long idAplicacion) {
        return formulario(aplicacion(idAplicacion));
    }

    @Transactional
    public Guardado guardar(Long idAplicacion, Long idCuestionario, List<RespuestaNueva> nuevas) {
        AplicacionEntity aplicacion = aplicacion(idAplicacion);
        CuestionarioEntity cuestionario = cuestionariosDe(aplicacion).stream()
                .filter(c -> c.getId().equals(idCuestionario))
                .findFirst()
                .orElseThrow(() -> new BusinessException("El cuestionario " + idCuestionario
                        + " no corresponde al grupo ocupacional de la aplicación " + idAplicacion));

        Map<Long, DimensionCuestionarioEntity> dimensiones = porId(
                dimensionCuestionarioRepository.findByFkCuestionario(cuestionario.getId()), DimensionCuestionarioEntity::getId);
        Map<Long, PreguntaEntity> preguntas = porId(
                preguntaRepository.findByFkDimensionCuestionarioIn(dimensiones.keySet()), PreguntaEntity::getId);
        Map<List<Long>, BigDecimal> valores = escalaDetalleRepository.findAll().stream()
                .collect(Collectors.toMap(e -> List.of(e.getFkEscala(), e.getFkOpcionRespuesta()), EscalaDetalleEntity::getValor));

        validar(aplicacion, cuestionario, nuevas, preguntas, dimensiones, valores);

        List<RespuestaEntity> anteriores = respuestaRepository.findByFkAplicacion(idAplicacion).stream()
                .filter(r -> preguntas.containsKey(r.getFkPregunta()))
                .toList();
        Map<Long, Long> antes = anteriores.stream()
                .collect(Collectors.toMap(RespuestaEntity::getFkPregunta, RespuestaEntity::getFkOpcionRespuesta));
        Map<Long, Long> despues = nuevas.stream()
                .collect(Collectors.toMap(RespuestaNueva::fkPregunta, RespuestaNueva::fkOpcionRespuesta));
        if (antes.equals(despues)) {
            return new Guardado(formulario(aplicacion), false, false);
        }

        // Borrado directo antes de insertar, para no chocar con el UNIQUE (aplicación, pregunta).
        respuestaRepository.deleteAllInBatch(anteriores);
        respuestaRepository.saveAll(nuevas.stream()
                .map(n -> {
                    PreguntaEntity pregunta = preguntas.get(n.fkPregunta());
                    RespuestaEntity respuesta = new RespuestaEntity();
                    respuesta.setFkAplicacion(idAplicacion);
                    respuesta.setFkPregunta(n.fkPregunta());
                    respuesta.setFkOpcionRespuesta(n.fkOpcionRespuesta());
                    respuesta.setValorObtenido(valores.get(List.of(pregunta.getFkEscala(), n.fkOpcionRespuesta())));
                    return respuesta;
                })
                .toList());
        boolean resultadosBorrados = borradoResultados.borrar(idAplicacion);

        aplicacion.setFechaModificado(LocalDateTime.now());
        aplicacion.setUsuarioModifica(UsuarioActual.nombre());
        aplicacionRepository.save(aplicacion);
        return new Guardado(formulario(aplicacion), true, resultadosBorrados);
    }

    /** Cada error con el número de la pregunta, para corregirlos todos de una vez. */
    private void validar(AplicacionEntity aplicacion, CuestionarioEntity cuestionario, List<RespuestaNueva> nuevas,
                         Map<Long, PreguntaEntity> preguntas, Map<Long, DimensionCuestionarioEntity> dimensiones,
                         Map<List<Long>, BigDecimal> valores) {
        List<String> errores = new ArrayList<>();
        Set<Long> vistas = new HashSet<>();
        for (RespuestaNueva nueva : nuevas) {
            PreguntaEntity pregunta = preguntas.get(nueva.fkPregunta());
            if (pregunta == null) {
                errores.add("La pregunta " + nueva.fkPregunta() + " no es del cuestionario " + cuestionario.getForma());
                continue;
            }
            String cual = "Pregunta " + pregunta.getNumero() + ": ";
            if (!vistas.add(pregunta.getId())) {
                errores.add(cual + "viene más de una vez");
            }
            if (!valores.containsKey(List.of(pregunta.getFkEscala(), nueva.fkOpcionRespuesta()))) {
                errores.add(cual + "la opción " + nueva.fkOpcionRespuesta() + " no es de su escala");
            }
            CondicionAplicacion condicion = dimensiones.get(pregunta.getFkDimensionCuestionario()).getCondicionAplicacion();
            if (!aplica(condicion, aplicacion)) {
                errores.add(cual + "no aplica (la pregunta filtro de la aplicación es \"no\")");
            }
        }
        if (!errores.isEmpty()) {
            throw new ValidationException("Las respuestas del cuestionario " + cuestionario.getForma() + " tienen errores", errores);
        }
    }

    private FormularioAplicacion formulario(AplicacionEntity aplicacion) {
        List<CuestionarioEntity> cuestionarios = cuestionariosDe(aplicacion);
        Map<Long, List<DimensionCuestionarioEntity>> dimensionesPorCuestionario = dimensionCuestionarioRepository.findAll().stream()
                .collect(Collectors.groupingBy(DimensionCuestionarioEntity::getFkCuestionario));
        Map<Long, DimensionCuestionarioEntity> dimensiones = porId(
                dimensionesPorCuestionario.values().stream().flatMap(List::stream).toList(), DimensionCuestionarioEntity::getId);
        Map<Long, String> nombresOpcion = opcionRespuestaRepository.findAll().stream()
                .collect(Collectors.toMap(OpcionRespuestaEntity::getId, OpcionRespuestaEntity::getNombre));
        // Opciones de cada escala en el orden de las opciones (Siempre … Nunca).
        Map<Long, List<FormularioAplicacion.Opcion>> opcionesPorEscala = escalaDetalleRepository.findAll().stream()
                .sorted(Comparator.comparing(EscalaDetalleEntity::getFkOpcionRespuesta))
                .collect(Collectors.groupingBy(EscalaDetalleEntity::getFkEscala, Collectors.mapping(
                        e -> new FormularioAplicacion.Opcion(e.getFkOpcionRespuesta(), nombresOpcion.get(e.getFkOpcionRespuesta())),
                        Collectors.toList())));
        Map<Long, Long> respuestas = respuestaRepository.findByFkAplicacion(aplicacion.getId()).stream()
                .collect(Collectors.toMap(RespuestaEntity::getFkPregunta, RespuestaEntity::getFkOpcionRespuesta));

        List<FormularioAplicacion.Cuestionario> resultado = cuestionarios.stream()
                .map(c -> {
                    Set<Long> idsDimension = dimensionesPorCuestionario.getOrDefault(c.getId(), List.of()).stream()
                            .map(DimensionCuestionarioEntity::getId).collect(Collectors.toSet());
                    List<FormularioAplicacion.Pregunta> preguntas = preguntaRepository.findByFkDimensionCuestionarioIn(idsDimension).stream()
                            .sorted(Comparator.comparing(PreguntaEntity::getNumero))
                            .map(p -> {
                                CondicionAplicacion condicion = dimensiones.get(p.getFkDimensionCuestionario()).getCondicionAplicacion();
                                return new FormularioAplicacion.Pregunta(p.getId(), p.getNumero(), p.getDescripcion(),
                                        condicion == null ? null : condicion.name(), aplica(condicion, aplicacion),
                                        opcionesPorEscala.getOrDefault(p.getFkEscala(), List.of()), respuestas.get(p.getId()));
                            })
                            .toList();
                    List<FormularioAplicacion.Pregunta> aplicables = preguntas.stream().filter(FormularioAplicacion.Pregunta::aplica).toList();
                    int respondidas = (int) aplicables.stream().filter(p -> p.respuesta() != null).count();
                    return new FormularioAplicacion.Cuestionario(c.getId(), c.getForma(), c.getNombre(),
                            aplicables.size(), respondidas, preguntas);
                })
                .toList();

        return new FormularioAplicacion(aplicacion.getId(), aplicacion.getFkGrupoOcupacional(), aplicacion.getAtiendeClientes(),
                aplicacion.getEsJefe(), estado(aplicacion.getId(), respuestas.isEmpty(), resultado), resultado);
    }

    private EstadoAplicacion estado(Long idAplicacion, boolean sinRespuestas, List<FormularioAplicacion.Cuestionario> cuestionarios) {
        if (sinRespuestas) {
            return EstadoAplicacion.SIN_RESPUESTAS;
        }
        if (borradoResultados.tieneResultados(idAplicacion)) {
            return EstadoAplicacion.CALCULADA;
        }
        boolean completa = cuestionarios.stream().allMatch(c -> c.respondidas() == c.aplicables());
        return completa ? EstadoAplicacion.PENDIENTE_CALCULO : EstadoAplicacion.INCOMPLETA;
    }

    /** Los cuestionarios con baremos para el grupo ocupacional de la aplicación, en el orden de sus ids (A/B, C, D). */
    private List<CuestionarioEntity> cuestionariosDe(AplicacionEntity aplicacion) {
        Set<Long> delGrupo = baremoCuestionarioRepository.findByFkGrupoOcupacional(aplicacion.getFkGrupoOcupacional()).stream()
                .map(BaremoCuestionarioEntity::getFkCuestionario)
                .collect(Collectors.toSet());
        return cuestionarioRepository.findAll().stream()
                .filter(c -> delGrupo.contains(c.getId()))
                .sorted(Comparator.comparing(CuestionarioEntity::getId))
                .toList();
    }

    /** Sin condición, o con la pregunta filtro en "sí" o sin registrar: se responde. Solo "no" la excluye. */
    private static boolean aplica(CondicionAplicacion condicion, AplicacionEntity aplicacion) {
        if (condicion == null) {
            return true;
        }
        Boolean filtro = switch (condicion) {
            case ATIENDE_CLIENTES -> aplicacion.getAtiendeClientes();
            case ES_JEFE -> aplicacion.getEsJefe();
        };
        return !Boolean.FALSE.equals(filtro);
    }

    private AplicacionEntity aplicacion(Long idAplicacion) {
        return aplicacionRepository.findById(idAplicacion)
                .orElseThrow(() -> new ResourceNotFoundException("Aplicación no encontrada con id: " + idAplicacion));
    }

    private static <T> Map<Long, T> porId(Collection<T> entidades, Function<T, Long> id) {
        return entidades.stream().collect(Collectors.toMap(id, Function.identity()));
    }

}
