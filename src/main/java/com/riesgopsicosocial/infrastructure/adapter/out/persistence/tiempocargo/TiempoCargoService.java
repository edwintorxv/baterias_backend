package com.riesgopsicosocial.infrastructure.adapter.out.persistence.tiempocargo;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class TiempoCargoService extends CatalogoCrudService<TiempoCargoEntity> {

    public TiempoCargoService(TiempoCargoJpaRepository repository){
        super(repository, "TiempoCargo");
    }
}
