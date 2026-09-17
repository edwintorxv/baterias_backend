package com.riesgopsicosocial.infrastructure.adapter.out.persistence.ciudadmunicipio;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CiudadMunicipioService extends CatalogoCrudService<CiudadMunicipioEntity> {

    private final CiudadMunicipioJpaResository ciudadMunicipioJpaResository;

    public CiudadMunicipioService(CiudadMunicipioJpaResository ciudadMunicipioJpaResository) {
        super(ciudadMunicipioJpaResository, "Ciudad municipio");
        this.ciudadMunicipioJpaResository = ciudadMunicipioJpaResository;
    }

    public List<CiudadMunicipioEntity> listarPorDepartamento(Long fkDepartamento) {
        return ciudadMunicipioJpaResository.findByFkDepartamento(fkDepartamento);
    }


}
