package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AplicacionJpaRepository extends JpaRepository<AplicacionEntity, Long> {

    List<AplicacionEntity> findByFkEvaluadoCliente(Long fkEvaluadoCliente);

    List<AplicacionEntity> findByEstado(String estado);

}
