package com.riesgopsicosocial.application.port.in.informe;

import com.riesgopsicosocial.domain.model.informe.empresa.InformeEmpresa;

import java.util.List;

public interface ConsultarInformeEmpresaUseCase {

    InformeEmpresa consultar(Long idCliente, int anio);

    /** Años con aplicaciones del cliente, del más reciente al más antiguo. */
    List<Integer> consultarAnios(Long idCliente);

}
