package com.riesgopsicosocial.application.service.informe;

import com.riesgopsicosocial.application.port.in.informe.ConsultarInformeEmpresaUseCase;
import com.riesgopsicosocial.application.port.in.informe.GenerarDocumentoInformeEmpresaUseCase;
import com.riesgopsicosocial.application.port.out.informe.GeneradorDocumentoInformeEmpresaPort;
import com.riesgopsicosocial.application.port.out.informe.InformeEmpresaPort;
import com.riesgopsicosocial.domain.model.informe.DatosEvaluador;
import com.riesgopsicosocial.domain.model.informe.DocumentoInforme;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.domain.model.informe.empresa.InformeEmpresa;
import com.riesgopsicosocial.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DocumentoInformeEmpresaService implements GenerarDocumentoInformeEmpresaUseCase {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final ConsultarInformeEmpresaUseCase consultarInforme;
    private final InformeEmpresaPort informeEmpresaPort;
    private final Map<FormatoDocumento, GeneradorDocumentoInformeEmpresaPort> generadores;

    public DocumentoInformeEmpresaService(ConsultarInformeEmpresaUseCase consultarInforme,
                                          InformeEmpresaPort informeEmpresaPort,
                                          List<GeneradorDocumentoInformeEmpresaPort> generadores) {
        this.consultarInforme = consultarInforme;
        this.informeEmpresaPort = informeEmpresaPort;
        this.generadores = generadores.stream()
                .collect(Collectors.toMap(GeneradorDocumentoInformeEmpresaPort::formato, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoInforme generar(Long idCliente, int anio, FormatoDocumento formato) {
        GeneradorDocumentoInformeEmpresaPort generador = generadores.get(formato);
        if (generador == null) {
            throw new BusinessException("El formato " + formato.extension() + " aún no está disponible para el informe de empresa");
        }
        InformeEmpresa informe = consultarInforme.consultar(idCliente, anio);
        // El profesional debe diligenciar las conclusiones y todo el plan de acción antes de descargar.
        List<String> faltantes = informe.conclusiones().faltantes();
        if (!faltantes.isEmpty()) {
            throw new BusinessException("Para descargar el informe de empresa de " + anio + " falta diligenciar: "
                    + String.join(", ", faltantes));
        }
        DatosEvaluador profesional = informe.conclusiones().profesional();
        FirmaEvaluador firma = profesional != null && profesional.tieneFirma()
                ? informeEmpresaPort.buscarFirma(profesional.id()).orElse(null) : null;
        byte[] contenido = generador.generar(informe, firma, LocalDate.now(ZONA));
        String nombreArchivo = "informe_empresa_" + informe.cliente().nit() + "_" + anio + "." + formato.extension();
        return new DocumentoInforme(nombreArchivo, formato, contenido);
    }

}
