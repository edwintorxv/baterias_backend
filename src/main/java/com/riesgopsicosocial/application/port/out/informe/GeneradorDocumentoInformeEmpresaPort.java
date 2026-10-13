package com.riesgopsicosocial.application.port.out.informe;

import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.domain.model.informe.empresa.InformeEmpresa;

import java.time.LocalDate;

/** Convierte el informe de empresa en un archivo; hay una implementación por formato. */
public interface GeneradorDocumentoInformeEmpresaPort {

    FormatoDocumento formato();

    byte[] generar(InformeEmpresa informe, LocalDate fechaElaboracion);

}
