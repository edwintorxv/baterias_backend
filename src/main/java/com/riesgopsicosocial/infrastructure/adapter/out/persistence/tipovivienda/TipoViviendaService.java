package com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipovivienda;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class TipoViviendaService extends CatalogoCrudService<TipoViviendaEntity> {

    public TipoViviendaService(TipoViviendaJpaRepository repository){
        super (repository, "TipoVivienda");
    }

}
