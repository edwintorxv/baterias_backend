package com.riesgopsicosocial.infrastructure.adapter.out.persistence.ciudadmunicipio;

import com.riesgopsicosocial.shared.catalogo.CatalogoJpaRepository;

import java.util.List;

public interface CiudadMunicipioJpaResository extends CatalogoJpaRepository<CiudadMunicipioEntity> {

    List<CiudadMunicipioEntity> findByFkDepartamento(Long fkDepartamento);



}
