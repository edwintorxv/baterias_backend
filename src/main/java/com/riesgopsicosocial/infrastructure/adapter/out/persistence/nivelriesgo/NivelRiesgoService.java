package com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelriesgo;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo.SexoEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo.SexoJpaRepository;
import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class NivelRiesgoService extends CatalogoCrudService<NivelRiesgoEntity> {

    public NivelRiesgoService(NivelRiesgoJpaRepository repository) {
        super(repository, "NivelRiesgo");
    }
}
