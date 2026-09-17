package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EvaluadoJpaRepository extends JpaRepository<EvaluadoEntity, Long> {

    boolean existsByNumeroIdentificacion(String numeroIdentificacion);

    Optional<EvaluadoEntity> findByNumeroIdentificacion(String numeroIdentificacion);

}
