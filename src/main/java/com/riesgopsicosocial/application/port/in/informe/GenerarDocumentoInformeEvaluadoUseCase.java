package com.riesgopsicosocial.application.port.in.informe;

import com.riesgopsicosocial.domain.model.informe.DocumentoInforme;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;

public interface GenerarDocumentoInformeEvaluadoUseCase {

    /**
     * Informe individual del año indicado según los formatos modelo de la batería. Exige que
     * cada aplicación del año tenga resultados calculados y evaluador asignado.
     */
    DocumentoInforme generar(Long idCliente, Long idEvaluado, int anio, FormatoDocumento formato);

}
