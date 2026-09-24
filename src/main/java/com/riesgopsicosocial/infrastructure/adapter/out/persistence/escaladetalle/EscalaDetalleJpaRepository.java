package com.riesgopsicosocial.infrastructure.adapter.out.persistence.escaladetalle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EscalaDetalleJpaRepository extends JpaRepository<EscalaDetalleEntity, Long> {

    Optional<EscalaDetalleEntity> findByFkEscalaAndFkOpcionRespuesta(Long fkEscala, Long fkOpcionRespuesta);

}
