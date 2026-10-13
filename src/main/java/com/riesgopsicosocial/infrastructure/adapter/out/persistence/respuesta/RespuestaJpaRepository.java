package com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta;

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

}
