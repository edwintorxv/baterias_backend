package com.riesgopsicosocial.infrastructure.adapter.out.persistence.cuestionario;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class CuestionarioService extends CatalogoCrudService<CuestionarioEntity> {

    public CuestionarioService(CuestionarioJpaRepository repository) {

        super(repository, "Cuestionario");

    }

}
