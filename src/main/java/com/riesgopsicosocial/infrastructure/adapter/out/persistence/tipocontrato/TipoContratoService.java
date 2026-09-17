package com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocontrato;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import com.riesgopsicosocial.shared.catalogo.CatalogoJpaRepository;
import org.springframework.stereotype.Service;

@Service
public class TipoContratoService extends CatalogoCrudService<TipoContratoEntity> {

    public TipoContratoService(TipoContratoJpaRepository repository){
        super(repository, "Tipo contrato");
    }

}
