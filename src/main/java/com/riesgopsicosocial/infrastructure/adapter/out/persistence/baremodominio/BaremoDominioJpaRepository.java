package com.riesgopsicosocial.infrastructure.adapter.out.persistence.baremodominio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaremoDominioJpaRepository extends JpaRepository<BaremoDominioEntity, Long> {

    List<BaremoDominioEntity> findByFkDominioCuestionario(Long fkDominioCuestionario);

    List<BaremoDominioEntity> findByFkNivelRiesgo(Long fkNivelRiesgo);

}
