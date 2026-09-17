package com.riesgopsicosocial.infrastructure.adapter.out.persistence.pregunta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreguntaJpaRepository extends JpaRepository<PreguntaEntity, Long> {

    List<PreguntaEntity> findByFkDimensionCuestionario(Long fkDimensionCuestionario);

    List<PreguntaEntity> findByFkEscala(Long fkEscala);

}
