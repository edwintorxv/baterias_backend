package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EvaluadoClienteJpaRepository extends JpaRepository<EvaluadoClienteEntity, Long> {

    List<EvaluadoClienteEntity> findByFkEvaluado(Long fkEvaluado);

    List<EvaluadoClienteEntity> findByFkCliente(Long fkCliente);

    List<EvaluadoClienteEntity> findByFkClienteIn(Collection<Long> fksCliente);

    List<EvaluadoClienteEntity> findByFkClienteAndActivoTrue(Long fkCliente);

    List<EvaluadoClienteEntity> findByFkEvaluadoAndFkCliente(Long fkEvaluado, Long fkCliente);

    boolean existsByFkEvaluadoAndFkClienteAndActivoTrue(Long fkEvaluado, Long fkCliente);

    Optional<EvaluadoClienteEntity> findByFkEvaluadoAndFkClienteAndActivoTrue(Long fkEvaluado, Long fkCliente);


}
