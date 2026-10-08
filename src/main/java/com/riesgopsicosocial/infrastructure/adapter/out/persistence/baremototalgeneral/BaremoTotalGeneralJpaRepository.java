package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremototalgeneral;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaremoTotalGeneralJpaRepository extends JpaRepository<BaremoTotalGeneralEntity, Long> {

    List<BaremoTotalGeneralEntity> findByFkCuestionarioIntralaboral(Long fkCuestionarioIntralaboral);

}
