package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluadoClienteJpaRepository extends JpaRepository<EvaluadoClienteEntity, Long> {

    List<EvaluadoClienteEntity> findByFkEvaluado(Long fkEvaluado);

    List<EvaluadoClienteEntity> findByFkCliente(Long fkCliente);

    boolean existsByFkEvaluadoAndFkClienteAndActivoTrue(Long fkEvaluado, Long fkCliente);

    Optional<EvaluadoClienteEntity> findByFkEvaluadoAndFkClienteAndActivoTrue(Long fkEvaluado, Long fkCliente);


}
