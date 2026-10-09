package com.riesgopsicosocial.application.service.informe;

import com.riesgopsicosocial.application.port.in.informe.ConsultarInformeEvaluadoUseCase;
import com.riesgopsicosocial.application.port.out.informe.GeneradorDocumentoInformePort;
import com.riesgopsicosocial.application.port.out.informe.InformeEvaluadoPort;
import com.riesgopsicosocial.domain.model.informe.*;
import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoInformeEvaluadoServiceTest {

    private static final DatosEvaluador CON_FIRMA = evaluador(1L, true);
    private static final DatosEvaluador SIN_FIRMA = evaluador(2L, false);
    private static final ResultadoAplicacion RESULTADO = new ResultadoAplicacion(1L, 1L, null, List.of(), null);
    private static final FirmaEvaluador FIRMA = new FirmaEvaluador(new byte[]{1, 2, 3}, "image/png");

    private final List<AplicacionEvaluado> aplicaciones = new ArrayList<>();
    private final GeneradorFalso generador = new GeneradorFalso();
    private final DocumentoInformeEvaluadoService service = new DocumentoInformeEvaluadoService(
            new ConsultaFalsa(), new PuertoFalso(), List.of(generador));

    @Test
    void generaElArchivoConNombreYFirmasDeLosEvaluadores() {
        aplicaciones.add(aplicacion(10L, CON_FIRMA, RESULTADO));
        aplicaciones.add(aplicacion(11L, SIN_FIRMA, RESULTADO));

        DocumentoInforme documento = service.generar(1L, 7L, 2026, FormatoDocumento.PDF);

        assertEquals("informe_123_2026.pdf", documento.nombreArchivo());
        assertArrayEquals(new byte[]{9}, documento.contenido());
        assertEquals(Set.of(1L), generador.firmasRecibidas.keySet());
    }

    @Test
    void aplicacionSinEvaluadorOSinResultados_lanza409ConTodosLosProblemas() {
        aplicaciones.add(aplicacion(10L, null, RESULTADO));
        aplicaciones.add(aplicacion(11L, CON_FIRMA, null));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.generar(1L, 7L, 2026, FormatoDocumento.PDF));
        assertTrue(ex.getMessage().contains("la aplicación 10 no tiene evaluador asignado"));
        assertTrue(ex.getMessage().contains("la aplicación 11 no tiene resultados calculados"));
    }

    @Test
    void formatoSinGenerador_lanza409() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.generar(1L, 7L, 2026, FormatoDocumento.DOCX));
        assertTrue(ex.getMessage().contains("DOCX"));
    }

    private static DatosEvaluador evaluador(Long id, boolean tieneFirma) {
        return new DatosEvaluador(id, "52" + id, "Evaluador " + id, "Psicóloga", null, "TP", "LSO",
                LocalDate.of(2020, 1, 1), tieneFirma);
    }

    private static AplicacionEvaluado aplicacion(Long id, DatosEvaluador evaluador, ResultadoAplicacion resultado) {
        return new AplicacionEvaluado(new DatosAplicacion(id, LocalDateTime.of(2026, 3, 1, 8, 0), "Analista",
                "Talento humano", 1L, "Grupo 1", evaluador, null, null), 36, resultado);
    }

    private class ConsultaFalsa implements ConsultarInformeEvaluadoUseCase {
        @Override
        public InformeEvaluado consultar(Long idCliente, Long idEvaluado, Integer anio) {
            return new InformeEvaluado(new DatosCliente(1L, "900", "Empresa X"),
                    new DatosEvaluado(7L, "123", "Ana", "Pérez", "Femenino", 1990), aplicaciones);
        }

        @Override
        public List<Integer> consultarAnios(Long idCliente, Long idEvaluado) {
            throw new UnsupportedOperationException();
        }
    }

    private static class PuertoFalso implements InformeEvaluadoPort {
        @Override
        public Optional<DatosCliente> buscarCliente(Long idCliente) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<DatosEvaluado> buscarEvaluado(Long idEvaluado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existeRelacion(Long idCliente, Long idEvaluado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DatosAplicacion> buscarAplicaciones(Long idCliente, Long idEvaluado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FirmaEvaluador> buscarFirma(Long idEvaluador) {
            return idEvaluador.equals(CON_FIRMA.id()) ? Optional.of(FIRMA) : Optional.empty();
        }
    }

    private static class GeneradorFalso implements GeneradorDocumentoInformePort {
        Map<Long, FirmaEvaluador> firmasRecibidas;

        @Override
        public FormatoDocumento formato() {
            return FormatoDocumento.PDF;
        }

        @Override
        public byte[] generar(InformeEvaluado informe, Map<Long, FirmaEvaluador> firmas, LocalDate fechaElaboracion) {
            firmasRecibidas = firmas;
            return new byte[]{9};
        }
    }

}
