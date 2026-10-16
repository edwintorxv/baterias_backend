package com.riesgopsicosocial.application.port.out.informe;

import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.domain.model.informe.empresa.InformeEmpresa;

import java.time.LocalDate;

/** Convierte el informe de empresa en un archivo; hay una implementación por formato. */
public interface GeneradorDocumentoInformeEmpresaPort {

    FormatoDocumento formato();

    /**
     * @param firma firma del profesional responsable; {@code null} si no hay profesional o no cargó su firma
     */
    byte[] generar(InformeEmpresa informe, FirmaEvaluador firma, LocalDate fechaElaboracion);

}
