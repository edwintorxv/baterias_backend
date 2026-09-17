package com.riesgopsicosocial.infrastructure.adapter.out.persistence.sectoreconimico;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class SectorEconomicoService extends CatalogoCrudService<SectorEconomicoEntity> {

    public SectorEconomicoService(SectorEconomicoJpaRepository repository) {

        super(repository, "Sector economico");

    }


}
