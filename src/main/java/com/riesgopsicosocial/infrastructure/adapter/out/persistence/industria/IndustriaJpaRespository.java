package com.riesgopsicosocial.infrastructure.adapter.out.persistence.industria;

import com.riesgopsicosocial.shared.catalogo.CatalogoJpaRepository;

import java.util.List;

public interface IndustriaJpaRespository extends CatalogoJpaRepository<IndustriaEntity> {

    List<IndustriaEntity> findByFkSectorEconomico(Long fkSectorEconomico);

}
