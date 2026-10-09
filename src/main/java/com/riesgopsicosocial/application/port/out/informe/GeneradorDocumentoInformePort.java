package com.riesgopsicosocial.application.port.out.informe;

import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.domain.model.informe.InformeEvaluado;

import java.time.LocalDate;
import java.util.Map;

/** Convierte el informe en un archivo; hay una implementación por formato. */
public interface GeneradorDocumentoInformePort {

    FormatoDocumento formato();

    /** @param firmas firma de cada evaluador del informe, por id; puede faltar si no la ha cargado */
    byte[] generar(InformeEvaluado informe, Map<Long, FirmaEvaluador> firmas, LocalDate fechaElaboracion);

}
