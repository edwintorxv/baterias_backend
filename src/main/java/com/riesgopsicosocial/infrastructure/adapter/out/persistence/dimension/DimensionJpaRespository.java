package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dimension;

import com.riesgopsicosocial.shared.catalogo.CatalogoJpaRepository;

public interface DimensionJpaRespository extends CatalogoJpaRepository<DimensionEntity> {

    boolean existsByFkDominioAndNombreIgnoreCase(Long fkDominio, String nombre);

}
