package com.riesgopsicosocial.infrastructure.adapter.out.persistence.tipocargo;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.sexo.SexoEntity;
import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class TipoCargoService extends CatalogoCrudService<TipoCargoEntity> {

    public TipoCargoService(TipoCargoJpaRepository repository) {
        super(repository, "Tipo Cargo");
    }

}
