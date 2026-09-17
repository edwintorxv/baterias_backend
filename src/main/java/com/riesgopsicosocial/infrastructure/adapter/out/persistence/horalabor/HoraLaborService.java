package com.riesgopsicosocial.infrastructure.adapter.out.persistence.horalabor;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class HoraLaborService extends CatalogoCrudService<HoraLaborEntity> {

    public HoraLaborService(HoraLaborJpaRepository repository){
        super(repository, "Horas Labor");
    }


}
