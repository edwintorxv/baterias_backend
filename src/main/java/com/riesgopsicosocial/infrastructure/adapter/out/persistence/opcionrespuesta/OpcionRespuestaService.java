package com.riesgopsicosocial.infrastructure.adapter.out.persistence.opcionrespuesta;

import com.riesgopsicosocial.shared.catalogo.CatalogoCrudService;
import org.springframework.stereotype.Service;

@Service
public class OpcionRespuestaService extends CatalogoCrudService<OpcionRespuestaEntity> {

    public OpcionRespuestaService(OpcionRespuestaJpaRepository repository){
        super(repository, "Opcion Respuesta");
    }


}
