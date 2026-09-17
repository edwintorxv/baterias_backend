package com.riesgopsicosocial.infrastructure.adapter.out.persistence.departamento;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class DepartamentoService extends CatalogoCrudService<DepartamentoEntity> {

    public DepartamentoService(DepartamentoJpaRepository repository) {
        super(repository, "Departamento");
    }
}
