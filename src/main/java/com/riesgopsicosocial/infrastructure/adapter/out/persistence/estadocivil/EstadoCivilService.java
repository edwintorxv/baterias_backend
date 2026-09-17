package com.riesgopsicosocial.infrastructure.adapter.out.persistence.estadocivil;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class EstadoCivilService extends CatalogoCrudService<EstadoCivilEntity> {

    public EstadoCivilService(EstadoCivilJpaRepository repository) {
        super(repository, "EstadoCivil");
    }

}
