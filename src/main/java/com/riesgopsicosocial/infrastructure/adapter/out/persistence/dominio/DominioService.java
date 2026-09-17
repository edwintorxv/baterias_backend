package com.riesgopsicosocial.infrastructure.adapter.out.persistence.dominio;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class DominioService extends CatalogoCrudService<DominioEntity> {

    public DominioService(DominioJpaRepository repository) {
        super(repository, "Dominio");

    }

}
