package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import com.riesgopsicosocial.domain.model.informe.*;
import com.riesgopsicosocial.domain.model.resultado.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Genera el informe en ambos formatos y verifica que digan lo mismo (sin BD ni Spring). */
class GeneradoresInformeEvaluadoTest {

    private static final NivelRiesgo BAJO = new NivelRiesgo(2L, "Riesgo bajo");
    private static final NivelRiesgo MUY_ALTO = new NivelRiesgo(5L, "Riesgo muy alto");

    /** Lo que debe aparecer en los dos documentos. */
    private static final List<String> TEXTOS_ESPERADOS = List.of(
            "Ana Pérez", "123", "Analista", "Talento humano", "36 años", "Femenino", "01/03/2026",
            "Empresa X (NIT 900)", "Psicóloga Uno", "TP-1", "LSO-1", "15/05/2020",
            "INTRALABORAL – FORMA A", "Liderazgo y relaciones sociales en el trabajo", "Características del liderazgo",
            "57,7", "LIDERAZGO Y RELACIONES SOCIALES EN EL TRABAJO", "46,3",
            "EXTRALABORAL", "Desplazamiento vivienda – trabajo – vivienda", "54,8",
            "Nivel de síntomas de estrés", "57,4", "Muy alto",
            "intralaboral forma A + extralaboral", "48,1",
            "Riesgo muy alto", "Riesgo bajo", "Observación 1", "Recomendación 1", "08/10/2026");

    private final InformeVistaMapper mapper = new InformeVistaMapper();

    @Test
    void pdf_contieneLosDatosDelFormato() throws Exception {
        byte[] pdf = new PdfInformeEvaluadoGenerator(mapper).generar(informe(), Map.of(), LocalDate.of(2026, 10, 8));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
            TEXTOS_ESPERADOS.forEach(esperado -> assertTrue(texto.contains(esperado), "falta en el PDF: " + esperado));
        }
    }

    @Test
    void docx_contieneLosDatosDelFormato() throws Exception {
        byte[] docx = new DocxInformeEvaluadoGenerator(mapper).generar(informe(), Map.of(), LocalDate.of(2026, 10, 8));

        try (XWPFDocument documento = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor extractor = new XWPFWordExtractor(documento)) {
            String texto = extractor.getText().replaceAll("\\s+", " ");
            TEXTOS_ESPERADOS.forEach(esperado -> assertTrue(texto.contains(esperado), "falta en el Word: " + esperado));
            assertTrue(texto.contains("CONFIDENCIAL"), "falta el encabezado de confidencialidad");
        }
    }

    private static InformeEvaluado informe() {
        DatosEvaluador evaluador = new DatosEvaluador(1L, "52", "Psicóloga Uno", "Psicóloga", "Especialista SO",
                "TP-1", "LSO-1", LocalDate.of(2020, 5, 15), false);
        DatosAplicacion datos = new DatosAplicacion(1L, LocalDateTime.of(2026, 3, 1, 8, 0), "Analista", "Talento humano",
                1L, "Jefes, profesionales y técnicos", evaluador, "Observación 1", "Recomendación 1");

        ResultadoCuestionario a = new ResultadoCuestionario(1L, "A", bd("228"), bd("46.3"), MUY_ALTO,
                List.of(new ResultadoDominio(10L, "Liderazgo y relaciones sociales en el trabajo", bd("80"), bd("52.4"), MUY_ALTO)),
                List.of(new ResultadoDimension(100L, 10L, "Características del liderazgo", bd("30"), bd("57.7"), MUY_ALTO),
                        new ResultadoDimension(101L, 10L, "Retroalimentación del desempeño", bd("7"), bd("35.0"), BAJO)));
        ResultadoCuestionario c = new ResultadoCuestionario(3L, "C", bd("68"), bd("54.8"), MUY_ALTO, List.of(),
                List.of(new ResultadoDimension(200L, null, "Desplazamiento vivienda – trabajo – vivienda", bd("8"), bd("50.0"), MUY_ALTO)));
        ResultadoCuestionario d = new ResultadoCuestionario(4L, "D", bd("35.08"), bd("57.4"), MUY_ALTO, List.of(), List.of());
        ResultadoAplicacion resultado = new ResultadoAplicacion(1L, 1L, LocalDateTime.of(2026, 10, 7, 20, 0),
                List.of(a, c, d), new ResultadoTotalGeneral(1L, "A", bd("296"), bd("48.1"), MUY_ALTO));

        return new InformeEvaluado(new DatosCliente(1L, "900", "Empresa X"),
                new DatosEvaluado(7L, "123", "Ana", "Pérez", "Femenino", 1990),
                List.of(new AplicacionEvaluado(datos, 36, resultado)));
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

}
