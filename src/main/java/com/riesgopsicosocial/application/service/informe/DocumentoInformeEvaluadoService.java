package com.riesgopsicosocial.application.service.informe;

import com.riesgopsicosocial.application.port.in.informe.ConsultarInformeEvaluadoUseCase;
import com.riesgopsicosocial.application.port.in.informe.GenerarDocumentoInformeEvaluadoUseCase;
import com.riesgopsicosocial.application.port.out.informe.GeneradorDocumentoInformePort;
import com.riesgopsicosocial.application.port.out.informe.InformeEvaluadoPort;
import com.riesgopsicosocial.domain.model.informe.*;
import com.riesgopsicosocial.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DocumentoInformeEvaluadoService implements GenerarDocumentoInformeEvaluadoUseCase {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final ConsultarInformeEvaluadoUseCase consultarInforme;
    private final InformeEvaluadoPort informeEvaluadoPort;
    private final Map<FormatoDocumento, GeneradorDocumentoInformePort> generadores;

    public DocumentoInformeEvaluadoService(ConsultarInformeEvaluadoUseCase consultarInforme,
                                           InformeEvaluadoPort informeEvaluadoPort,
                                           List<GeneradorDocumentoInformePort> generadores) {
        this.consultarInforme = consultarInforme;
        this.informeEvaluadoPort = informeEvaluadoPort;
        this.generadores = generadores.stream()
                .collect(Collectors.toMap(GeneradorDocumentoInformePort::formato, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoInforme generar(Long idCliente, Long idEvaluado, int anio, FormatoDocumento formato) {
        GeneradorDocumentoInformePort generador = generadores.get(formato);
        if (generador == null) {
            throw new BusinessException("El formato " + formato + " aún no está disponible para el informe individual");
        }

        InformeEvaluado informe = consultarInforme.consultar(idCliente, idEvaluado, anio);
        validarCompleto(informe);

        Map<Long, FirmaEvaluador> firmas = new HashMap<>();
        informe.aplicaciones().stream()
                .map(a -> a.datos().evaluador())
                .filter(DatosEvaluador::tieneFirma)
                .map(DatosEvaluador::id)
                .distinct()
                .forEach(id -> informeEvaluadoPort.buscarFirma(id).ifPresent(f -> firmas.put(id, f)));

        byte[] contenido = generador.generar(informe, firmas, LocalDate.now(ZONA));
        String nombreArchivo = "informe_" + informe.evaluado().numeroIdentificacion() + "_" + anio + "." + formato.extension();
        return new DocumentoInforme(nombreArchivo, formato, contenido);
    }

    /** El manual exige los datos del evaluador; sin resultados no hay nada que informar. */
    private void validarCompleto(InformeEvaluado informe) {
        List<String> problemas = new ArrayList<>();
        for (AplicacionEvaluado aplicacion : informe.aplicaciones()) {
            Long id = aplicacion.datos().idAplicacion();
            if (!aplicacion.tieneResultados()) {
                problemas.add("la aplicación " + id + " no tiene resultados calculados");
            }
            if (aplicacion.datos().evaluador() == null) {
                problemas.add("la aplicación " + id + " no tiene evaluador asignado (el informe no es válido sin sus datos)");
            }
        }
        if (!problemas.isEmpty()) {
            throw new BusinessException("No se puede generar el informe: " + String.join("; ", problemas));
        }
    }

}
