package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluador;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluadorJpaRepository extends JpaRepository<EvaluadorEntity, Long> {

    boolean existsByNumeroIdentificacion(String numeroIdentificacion);

    Optional<EvaluadorEntity> findByNumeroIdentificacion(String numeroIdentificacion);

    List<EvaluadorEntity> findByActivo(Boolean activo);

}
