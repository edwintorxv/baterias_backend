package com.riesgopsicosocial.infrastructure.adapter.out.persistence.informe;

import com.riesgopsicosocial.domain.model.informe.empresa.DimensionInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.ItemSintoma;
import com.riesgopsicosocial.domain.model.informe.empresa.OpcionFrecuencia;
import com.riesgopsicosocial.domain.model.informe.empresa.RespuestaSintoma;
import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario.CuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension.DimensionJpaRespository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario.DimensionCuestionarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta.OpcionRespuestaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta.PreguntaJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.RespuestaJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuración y respuestas del cuestionario de estrés (el de promedio ponderado) para los síntomas de
 * los dos informes: el de empresa (frecuencias) y el individual (síntomas reportados con frecuencia).
 */
@Component
class SintomasEstresLoader {

    private final CuestionarioJpaRepository cuestionarioRepository;
    private final DimensionCuestionarioJpaRepository dimensionCuestionarioRepository;
    private final DimensionJpaRespository dimensionRepository;
    private final PreguntaJpaRepository preguntaRepository;
    private final OpcionRespuestaJpaRepository opcionRespuestaRepository;
    private final RespuestaJpaRepository respuestaRepository;

    SintomasEstresLoader(CuestionarioJpaRepository cuestionarioRepository,
                         DimensionCuestionarioJpaRepository dimensionCuestionarioRepository,
                         DimensionJpaRespository dimensionRepository,
                         PreguntaJpaRepository preguntaRepository,
                         OpcionRespuestaJpaRepository opcionRespuestaRepository,
                         RespuestaJpaRepository respuestaRepository) {
        this.cuestionarioRepository = cuestionarioRepository;
        this.dimensionCuestionarioRepository = dimensionCuestionarioRepository;
        this.dimensionRepository = dimensionRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRespuestaRepository = opcionRespuestaRepository;
        this.respuestaRepository = respuestaRepository;
    }

    /** Dimensiones del cuestionario de estrés en el orden de sus ids (el del cuestionario: fisiológicos primero). */
    List<DimensionInforme> buscarDimensiones() {
        Map<Long, String> nombres = dimensionRepository.findAll().stream()
                .collect(Collectors.toMap(DimensionEntity::getId, DimensionEntity::getNombre));
        return dimensionesEstres().stream()
                .sorted(Comparator.comparing(DimensionCuestionarioEntity::getId))
                .map(dc -> new DimensionInforme(dc.getId(), nombres.get(dc.getFkDimension()), dc.getCondicionAplicacion()))
                .toList();
    }

    /** Preguntas de las dimensiones del cuestionario de estrés. */
    List<ItemSintoma> buscarItems() {
        Set<Long> dimensiones = dimensionesEstres().stream()
                .map(DimensionCuestionarioEntity::getId)
                .collect(Collectors.toSet());
        if (dimensiones.isEmpty()) {
            return List.of();
        }
        return preguntaRepository.findByFkDimensionCuestionarioIn(dimensiones).stream()
                .map(p -> new ItemSintoma(p.getId(), p.getFkDimensionCuestionario(), p.getNumero(), p.getDescripcion()))
                .toList();
    }

    /** De la más frecuente (Siempre) a la menos (Nunca). */
    List<OpcionFrecuencia> buscarOpciones() {
        return opcionRespuestaRepository.buscarPorMetodoCalculo(MetodoCalculo.PROMEDIO_PONDERADO).stream()
                .map(o -> new OpcionFrecuencia(o.getId(), o.getNombre()))
                .toList();
    }

    List<RespuestaSintoma> buscarRespuestas(Collection<Long> idsAplicacion) {
        if (idsAplicacion.isEmpty()) {
            return List.of();
        }
        return respuestaRepository.buscarPorMetodoCalculo(idsAplicacion, MetodoCalculo.PROMEDIO_PONDERADO).stream()
                .map(r -> new RespuestaSintoma(r.getFkAplicacion(), r.getFkPregunta(), r.getFkDimensionCuestionario(),
                        r.getFkOpcionRespuesta()))
                .toList();
    }

    private List<DimensionCuestionarioEntity> dimensionesEstres() {
        Set<Long> estres = cuestionarioRepository.findAll().stream()
                .filter(c -> c.getMetodoCalculo() == MetodoCalculo.PROMEDIO_PONDERADO)
                .map(CuestionarioEntity::getId)
                .collect(Collectors.toSet());
        return dimensionCuestionarioRepository.findAll().stream()
                .filter(dc -> estres.contains(dc.getFkCuestionario()))
                .toList();
    }

}
