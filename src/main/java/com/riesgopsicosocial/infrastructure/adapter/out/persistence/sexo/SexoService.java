package com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class SexoService extends CatalogoCrudService<SexoEntity> {

    public SexoService(SexoJpaRepository repository) {
        super(repository, "Sexo");
    }
}
