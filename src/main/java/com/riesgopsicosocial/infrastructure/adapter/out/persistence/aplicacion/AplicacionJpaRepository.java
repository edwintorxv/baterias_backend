package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AplicacionJpaRepository extends JpaRepository<AplicacionEntity, Long> {

    List<AplicacionEntity> findByFkEvaluadoCliente(Long fkEvaluadoCliente);

    List<AplicacionEntity> findByFkEvaluadoClienteIn(Collection<Long> fksEvaluadoCliente);

    /** Aplicaciones de las relaciones indicadas con fecha en [desde, hasta). */
    @Query("select a from AplicacionEntity a where a.fkEvaluadoCliente in :fksEvaluadoCliente"
            + " and a.fechaAplicacion >= :desde and a.fechaAplicacion < :hasta")
    List<AplicacionEntity> buscarPorRelacionesYPeriodo(@Param("fksEvaluadoCliente") Collection<Long> fksEvaluadoCliente,
                                                       @Param("desde") LocalDateTime desde,
                                                       @Param("hasta") LocalDateTime hasta);

    List<AplicacionEntity> findByEstado(String estado);

}
