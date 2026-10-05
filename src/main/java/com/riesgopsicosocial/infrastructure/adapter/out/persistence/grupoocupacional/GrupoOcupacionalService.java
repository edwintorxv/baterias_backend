package com.riesgopsicosocial.infrastructure.adapter.out.persistence.grupoocupacional;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class GrupoOcupacionalService extends CatalogoCrudService<GrupoOcupacionalEntity> {

    public GrupoOcupacionalService(GrupoOcupacionalJpaRepository repository) {
        super(repository, "Grupo Ocupacional");
    }

}
