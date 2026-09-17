package com.riesgopsicosocial.infrastructure.adapter.out.persistence.nivelacademico;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class NivelAcademicoService extends CatalogoCrudService<NivelAcademicoEntity> {

    public NivelAcademicoService(NivelAcademicoJpaRepository repository) {
        super(repository, "NivelAcademico");
    }

}
