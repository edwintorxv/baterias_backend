package com.riesgopsicosocial.infrastructure.adapter.out.persistence.industria;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndustriaService extends CatalogoCrudService<IndustriaEntity> {

    private final IndustriaJpaRespository industriaJpaRespository;

    public IndustriaService(IndustriaJpaRespository respository) {
        super(respository, "Industria");
        this.industriaJpaRespository = respository;
    }

    public List<IndustriaEntity> listarPorSectorEconomico(Long fkSectorEconomico) {
        return industriaJpaRespository.findByFkSectorEconomico(fkSectorEconomico);
    }

}
