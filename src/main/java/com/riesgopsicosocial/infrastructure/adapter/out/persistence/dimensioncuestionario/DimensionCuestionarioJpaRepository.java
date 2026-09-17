package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimensioncuestionario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DimensionCuestionarioJpaRepository extends JpaRepository<DimensionCuestionarioEntity, Long> {

    List<DimensionCuestionarioEntity> findByFkDimension(Long fkDimension);

    List<DimensionCuestionarioEntity> findByFkCuestionario(Long fkCuestionario);

    boolean existsByFkDimensionAndFkCuestionario(Long fkDimension, Long fkCuestionario);

}
