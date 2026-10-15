package com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta;

import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import com.riesgopsicosocial.shared.catalogo.CatalogoJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OpcionRespuestaJpaRepository extends CatalogoJpaRepository<OpcionRespuestaEntity> {

    /** Opciones de las escalas que usan las preguntas de los cuestionarios con ese método de cálculo, por id. */
    @Query("""
            select o from OpcionRespuestaEntity o
            where o.id in (select ed.fkOpcionRespuesta from EscalaDetalleEntity ed, PreguntaEntity p,
                                DimensionCuestionarioEntity dc, CuestionarioEntity c
                           where ed.fkEscala = p.fkEscala and dc.id = p.fkDimensionCuestionario
                             and c.id = dc.fkCuestionario and c.metodoCalculo = :metodo)
            order by o.id""")
    List<OpcionRespuestaEntity> buscarPorMetodoCalculo(@Param("metodo") MetodoCalculo metodo);

}
