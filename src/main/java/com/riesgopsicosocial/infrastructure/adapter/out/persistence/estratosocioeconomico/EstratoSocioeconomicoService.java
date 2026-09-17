package com.riesgopsicosocial.infrastructure.adapter.out.persistence.estratosocioeconomico;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class EstratoSocioeconomicoService extends CatalogoCrudService<EstratoSocioeconomicoEntity> {

    public EstratoSocioeconomicoService(EstratoSocioeconomicoJpaRepository repository){
        super(repository, "EstratoSocioeconomico");
    }

}
