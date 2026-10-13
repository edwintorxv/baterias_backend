package com.riesgopsicosocial.application.port.in.informe;

import com.riesgopsicosocial.domain.model.informe.DocumentoInforme;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;

public interface GenerarDocumentoInformeEmpresaUseCase {

    DocumentoInforme generar(Long idCliente, int anio, FormatoDocumento formato);

}
