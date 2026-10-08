package com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultado;

import com.riesgopsicosocial.application.port.out.resultado.DatosCalculoPort;
import com.riesgopsicosocial.domain.model.resultado.configuracion.*;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion.AplicacionJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaJpaRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DatosCalculoPersistenceAdapter implements DatosCalculoPort {

    private final AplicacionJpaRepository aplicacionRepository;
    private final RespuestaJpaRepository respuestaRepository;
    private final PreguntaJpaRepository preguntaRepository;
    private final DimensionCuestionarioJpaRepository dimensionCuestionarioRepository;
    private final CatalogoCalculoLoader catalogo;

    public DatosCalculoPersistenceAdapter(AplicacionJpaRepository aplicacionRepository,
                                          RespuestaJpaRepository respuestaRepository,
                                          PreguntaJpaRepository preguntaRepository,
                                          DimensionCuestionarioJpaRepository dimensionCuestionarioRepository,
                                          CatalogoCalculoLoader catalogo) {
        this.aplicacionRepository = aplicacionRepository;
        this.respuestaRepository = respuestaRepository;
        this.preguntaRepository = preguntaRepository;
        this.dimensionCuestionarioRepository = dimensionCuestionarioRepository;
        this.catalogo = catalogo;
    }

    @Override
    public Optional<Long> buscarGrupoOcupacional(Long idAplicacion) {
        return aplicacionRepository.findById(idAplicacion).map(AplicacionEntity::getFkGrupoOcupacional);
    }

    @Override
    public FiltrosAplicacion buscarFiltros(Long idAplicacion) {
        return aplicacionRepository.findById(idAplicacion)
                .map(a -> new FiltrosAplicacion(a.getAtiendeClientes(), a.getEsJefe()))
                .orElse(FiltrosAplicacion.SIN_REGISTRO);
    }

    @Override
    public List<RespuestaCalculo> buscarRespuestas(Long idAplicacion) {
        List<RespuestaEntity> respuestas = respuestaRepository.findByFkAplicacion(idAplicacion);
        if (respuestas.isEmpty()) {
            return List.of();
        }

        Map<Long, PreguntaEntity> preguntas = porId(
                preguntaRepository.findAllById(respuestas.stream().map(RespuestaEntity::getFkPregunta).collect(Collectors.toSet())),
                PreguntaEntity::getId);
        Map<Long, DimensionCuestionarioEntity> dimensiones = porId(
                dimensionCuestionarioRepository.findAllById(preguntas.values().stream()
                        .map(PreguntaEntity::getFkDimensionCuestionario).collect(Collectors.toSet())),
                DimensionCuestionarioEntity::getId);

        return respuestas.stream()
                .map(r -> {
                    PreguntaEntity pregunta = preguntas.get(r.getFkPregunta());
                    DimensionCuestionarioEntity dc = dimensiones.get(pregunta.getFkDimensionCuestionario());
                    return new RespuestaCalculo(pregunta.getId(), dc.getId(), dc.getFkCuestionario(), r.getValorObtenido());
                })
                .toList();
    }

    @Override
    public ConfiguracionCuestionario cargarConfiguracion(Long idCuestionario, Long idGrupoOcupacional) {
        return catalogo.cargarConfiguracion(idCuestionario, idGrupoOcupacional);
    }

    @Override
    public List<RangoBaremo> cargarBaremosTotalGeneral(Long idCuestionarioIntralaboral) {
        return catalogo.cargarBaremosTotalGeneral(idCuestionarioIntralaboral);
    }

    private static <T> Map<Long, T> porId(Iterable<T> entidades, Function<T, Long> id) {
        Map<Long, T> mapa = new HashMap<>();
        entidades.forEach(e -> mapa.put(id.apply(e), e));
        return mapa;
    }

}
