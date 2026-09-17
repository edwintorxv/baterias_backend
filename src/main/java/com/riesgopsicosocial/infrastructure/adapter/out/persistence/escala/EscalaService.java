package com.riesgopsicosocial.infrastructure.adapter.out.persistence.escala;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class EscalaService extends CatalogoCrudService<EscalaEntity> {

    public EscalaService(EscalaJpaRepository repository){
        super(repository, "Escala");
    }


}
