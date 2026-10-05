package com.riesgopsicosocial.infrastructure.adapter.out.persistence.resultadodominio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResultadoDominioJpaRepository extends JpaRepository<ResultadoDominioEntity, Long> {

    List<ResultadoDominioEntity> findByFkAplicacion(Long fkAplicacion);

    /** Borrado directo en BD (no carga entidades) para poder reinsertar sin chocar con el UNIQUE. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ResultadoDominioEntity r where r.fkAplicacion = :fkAplicacion")
    void eliminarPorAplicacion(@Param("fkAplicacion") Long fkAplicacion);

}
