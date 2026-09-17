package com.riesgopsicosocial.infrastructure.adapter.out.persistence.tiposalario;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class TipoSalarioService extends CatalogoCrudService<TipoSalarioEntity> {

    public TipoSalarioService(TipoSalarioJpaRepository repository) {
        super(repository, "Tipo salario");
    }

}
