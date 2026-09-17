package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominiocuestionario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DominioCuestionarioJpaRepository extends JpaRepository<DominioCuestionarioEntity, Long> {

    List<DominioCuestionarioEntity> findByFkDominio(Long fkDominio);

    List<DominioCuestionarioEntity> findByFkCuestionario(Long fkCuestionario);

    boolean existsByFkDominioAndFkCuestionario(Long fkDominio, Long fkCuestionario);

}
