package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodimension;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaremoDimensionJpaRepository extends JpaRepository<BaremoDimensionEntity, Long> {

    List<BaremoDimensionEntity> findByFkDimensionCuestionario(Long fkDimensionCuestionario);

    List<BaremoDimensionEntity> findByFkNivelRiesgo(Long fkNivelRiesgo);

}
