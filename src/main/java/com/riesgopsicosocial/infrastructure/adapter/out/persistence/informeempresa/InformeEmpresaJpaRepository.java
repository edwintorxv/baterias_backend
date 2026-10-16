package com.riesgopsicosocial.infrastructure.adapter.out.persistence.informeempresa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InformeEmpresaJpaRepository extends JpaRepository<InformeEmpresaEntity, Long> {

    Optional<InformeEmpresaEntity> findByFkClienteAndAnio(Long fkCliente, Integer anio);

}
