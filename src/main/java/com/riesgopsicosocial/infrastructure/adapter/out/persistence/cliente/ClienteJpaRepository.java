package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

    boolean existsByNit(String nit);

}
