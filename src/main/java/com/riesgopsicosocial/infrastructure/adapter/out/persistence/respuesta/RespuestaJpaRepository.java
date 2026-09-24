package com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RespuestaJpaRepository extends JpaRepository<RespuestaEntity, Long> {

    List<RespuestaEntity> findByFkAplicacion(Long fkAplicacion);

}
