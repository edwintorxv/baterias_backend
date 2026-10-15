package com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta;

import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface RespuestaJpaRepository extends JpaRepository<RespuestaEntity, Long> {

    List<RespuestaEntity> findByFkAplicacion(Long fkAplicacion);

    /** De las aplicaciones indicadas, las que tienen al menos una respuesta. */
    @Query("select distinct r.fkAplicacion from RespuestaEntity r where r.fkAplicacion in :fksAplicacion")
    Set<Long> buscarAplicacionesConRespuestas(@Param("fksAplicacion") Collection<Long> fksAplicacion);

    /** Respuesta reducida a su aplicación, su pregunta con la dimensión y la opción elegida. */
    interface RespuestaPorDimension {
        Long getFkAplicacion();

        Long getFkPregunta();

        Long getFkDimensionCuestionario();

        Long getFkOpcionRespuesta();
    }

    /** Respuestas de las aplicaciones indicadas a los cuestionarios con el método de cálculo indicado. */
    @Query("""
            select r.fkAplicacion as fkAplicacion, r.fkPregunta as fkPregunta,
                   p.fkDimensionCuestionario as fkDimensionCuestionario,
                   r.fkOpcionRespuesta as fkOpcionRespuesta
            from RespuestaEntity r, PreguntaEntity p, DimensionCuestionarioEntity dc, CuestionarioEntity c
            where p.id = r.fkPregunta and dc.id = p.fkDimensionCuestionario and c.id = dc.fkCuestionario
              and c.metodoCalculo = :metodo and r.fkAplicacion in :fksAplicacion""")
    List<RespuestaPorDimension> buscarPorMetodoCalculo(@Param("fksAplicacion") Collection<Long> fksAplicacion,
                                                       @Param("metodo") MetodoCalculo metodo);

}
