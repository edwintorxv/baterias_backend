package com.riesgopsicosocial.application.service.informe;

import com.riesgopsicosocial.application.port.in.informe.ConsultarInformeEmpresaUseCase;
import com.riesgopsicosocial.application.port.out.informe.GeneradorDocumentoInformeEmpresaPort;
import com.riesgopsicosocial.application.port.out.informe.InformeEmpresaPort;
import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.DatosEvaluador;
import com.riesgopsicosocial.domain.model.informe.DocumentoInforme;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.domain.model.informe.empresa.*;
import com.riesgopsicosocial.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentoInformeEmpresaServiceTest {

    private static final DatosEvaluador PROFESIONAL = new DatosEvaluador(7L, "1", "Psicóloga", "Psicología", null,
            "TP", "LSO", LocalDate.of(2020, 1, 1), true);

    private final ConsultarInformeEmpresaUseCase consultar = mock(ConsultarInformeEmpresaUseCase.class);
    private final InformeEmpresaPort puerto = mock(InformeEmpresaPort.class);
    private final GeneradorDocumentoInformeEmpresaPort generador = mock(GeneradorDocumentoInformeEmpresaPort.class);
    private final DocumentoInformeEmpresaService service;

    DocumentoInformeEmpresaServiceTest() {
        when(generador.formato()).thenReturn(FormatoDocumento.DOCX);
        service = new DocumentoInformeEmpresaService(consultar, puerto, List.of(generador));
    }

    @Test
    void sinConclusionesCompletas_409ConLoQueFalta_yNoGenera() {
        when(consultar.consultar(1L, 2026)).thenReturn(informe(new ConclusionesInforme("c", null, "m", null, null)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.generar(1L, 2026, FormatoDocumento.DOCX));

        assertTrue(error.getMessage().contains("acciones a corto plazo, acciones a largo plazo"), error.getMessage());
        verify(generador, never()).generar(any(), any(), any());
    }

    @Test
    void completas_generaConLaFirmaDelProfesional() {
        FirmaEvaluador firma = new FirmaEvaluador(new byte[]{1}, "image/png");
        InformeEmpresa informe = informe(new ConclusionesInforme("c", "corto", "mediano", "largo", PROFESIONAL));
        when(consultar.consultar(1L, 2026)).thenReturn(informe);
        when(puerto.buscarFirma(7L)).thenReturn(Optional.of(firma));
        when(generador.generar(eq(informe), eq(firma), any())).thenReturn(new byte[]{9});

        DocumentoInforme documento = service.generar(1L, 2026, FormatoDocumento.DOCX);

        assertEquals("informe_empresa_900_2026.docx", documento.nombreArchivo());
        assertArrayEquals(new byte[]{9}, documento.contenido());
    }

    private static InformeEmpresa informe(ConclusionesInforme conclusiones) {
        DistribucionNiveles vacia = new DistribucionNiveles(0, List.of());
        DesgloseGrupos sinGrupos = new DesgloseGrupos(List.of(), 0, 0);
        return new InformeEmpresa(new DatosCliente(1L, "900", "Empresa X"), 2026,
                new AnalisisGrupo(new Cobertura(1, 1, new BigDecimal("100.0")), List.of(), 0, vacia, vacia, vacia, vacia,
                        List.of()),
                new DescripcionPoblacion(0, List.of()),
                new ConcentracionRiesgo(List.of(), sinGrupos, sinGrupos, sinGrupos, sinGrupos),
                new ResultadosExtralaborales(List.of(), sinGrupos, sinGrupos),
                new SintomasEstres(0, List.of()), conclusiones);
    }

}
