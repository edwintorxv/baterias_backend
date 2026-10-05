package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremocuestionario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaremoCuestionarioJpaRepository extends JpaRepository<BaremoCuestionarioEntity, Long> {

    List<BaremoCuestionarioEntity> findByFkCuestionarioAndFkGrupoOcupacional(Long fkCuestionario, Long fkGrupoOcupacional);

}
