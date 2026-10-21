package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaremoCuestionarioJpaRepository extends JpaRepository<BaremoCuestionarioEntity, Long> {

    List<BaremoCuestionarioEntity> findByFkCuestionario(Long fkCuestionario);

    List<BaremoCuestionarioEntity> findByFkCuestionarioAndFkGrupoOcupacional(Long fkCuestionario, Long fkGrupoOcupacional);

    /** Los cuestionarios con baremos del grupo son los que le corresponden (A solo grupo 1, B solo grupo 2; C y D ambos). */
    List<BaremoCuestionarioEntity> findByFkGrupoOcupacional(Long fkGrupoOcupacional);

}
