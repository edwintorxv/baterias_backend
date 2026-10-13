package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.empresa.*;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFChart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DocxInformeEmpresaGeneratorTest {

    private static final NivelRiesgo BAJO = new NivelRiesgo(2L, "Riesgo bajo");
    private static final NivelRiesgo ALTO = new NivelRiesgo(4L, "Riesgo alto");

    private static List<String> categorias(XWPFChart grafico) {
        var datos = grafico.getChartSeries().get(0).getSeries(0).getCategoryData();
        return IntStream.range(0, datos.getPointCount()).mapToObj(i -> String.valueOf(datos.getPointAt(i))).toList();
    }

    /** Dimensión de 2 personas (una pregunta filtro dejó fuera a 1 de las 3 del dominio). */
    private static DistribucionDimension dimension(int numero) {
        DistribucionNiveles dos = new DistribucionNiveles(2, List.of(
                new ConteoNivel(BAJO, 1, new BigDecimal("50.0")), new ConteoNivel(ALTO, 1, new BigDecimal("50.0"))));
        return new DistribucionDimension((long) numero, "Dimensión " + numero, dos);
    }

    @Test
    void generaWordConCoberturaResultadosGlobalesDominiosYFichasDeDimensiones() throws Exception {
        DistribucionNiveles distribucion = new DistribucionNiveles(3, List.of(
                new ConteoNivel(BAJO, 1, new BigDecimal("33.3")), new ConteoNivel(ALTO, 2, new BigDecimal("66.7"))));
        AnalisisGrupo general = new AnalisisGrupo(new Cobertura(4, 3, new BigDecimal("75.0")),
                List.of(new CoberturaCiudad(1L, "BOGOTA, D.C.", new Cobertura(3, 3, new BigDecimal("100.0"))),
                        new CoberturaCiudad(null, "Sin ciudad", new Cobertura(1, 0, new BigDecimal("0.0")))),
                2,
                distribucion, distribucion, distribucion,
                List.of(new DominiosForma(1L, "A", 3, List.of(
                        new DistribucionDominio(10L, "Recompensas", distribucion, List.of(
                                new DistribucionDimension(100L, "Reconocimiento y compensación", distribucion))),
                        new DistribucionDominio(11L, "Liderazgo", distribucion, List.of(
                                dimension(5), dimension(4), dimension(3), dimension(2), dimension(1)))))));
        InformeEmpresa informe = new InformeEmpresa(new DatosCliente(1L, "900", "Empresa X"), 2026, general);

        byte[] word = new DocxInformeEmpresaGenerator().generar(informe, LocalDate.of(2026, 10, 13));

        String texto;
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(word));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            texto = extractor.getText();
            // Intralaboral, extralaboral, estrés, ranking de dominios y una ficha por dimensión (6).
            // La población por ciudad y el resumen de dimensiones no llevan gráfico.
            // getCharts() no garantiza el orden del documento: se identifican por sus categorías.
            assertEquals(10, doc.getCharts().size());
            List<List<String>> categorias = doc.getCharts().stream().map(DocxInformeEmpresaGeneratorTest::categorias).toList();
            assertEquals(8, categorias.stream().filter(List.of("Riesgo bajo", "Riesgo alto")::equals).count(),
                    "intralaboral, extralaboral y las 6 fichas usan el gráfico de barras por nivel");
            assertTrue(categorias.contains(List.of("Bajo", "Alto")), "estrés con las etiquetas del manual");
            assertTrue(categorias.contains(List.of("Recompensas", "Liderazgo")), "dominios en orden de ranking");

            XWPFTable resumen = doc.getTables().stream()
                    .filter(t -> t.getRow(0).getCell(0).getText().equals("Dimensión")).findFirst().orElseThrow();
            assertEquals(List.of("Reconocimiento y compensación", "Dimensión 5", "Dimensión 4", "Dimensión 3",
                            "Dimensión 2", "Dimensión 1"),
                    resumen.getRows().stream().skip(1).map(f -> f.getCell(0).getText()).toList(),
                    "todas las dimensiones de la forma, de la más crítica a la menos crítica");
            long fichas = doc.getTables().stream()
                    .filter(t -> t.getNumberOfRows() == 2 && t.getRow(0).getTableCells().size() == 5).count();
            assertEquals(6, fichas);
            List<Double> riesgoAlto = doc.getCharts().stream()
                    .filter(g -> categorias(g).equals(List.of("Riesgo bajo", "Riesgo alto")))
                    .map(g -> g.getChartSeries().get(0).getSeries(0).getValuesData().getPointAt(1).doubleValue())
                    .toList();
            assertEquals(3, riesgoAlto.stream().filter(v -> v == 66.7).count(), "intralaboral, extralaboral y Reconocimiento");
            assertEquals(5, riesgoAlto.stream().filter(v -> v == 50.0).count(), "las 5 dimensiones de Liderazgo");
        }
        for (String esperado : List.of("Empresa X", "900", "Año 2026", "13/10/2026", "POBLACIÓN EVALUADA", "Ciudad de trabajo", "BOGOTA, D.C.", "3 (100,0 %)", "Sin ciudad",
                "0 (0,0 %)", "3 (75,0 %)",
                "2 personas respondieron", "FACTORES DE RIESGO PSICOSOCIAL INTRALABORALES",
                "FACTORES DE RIESGO PSICOSOCIAL EXTRALABORALES", "VALORACIÓN DEL ESTRÉS",
                "Riesgo alto", "66,7 %", "Nivel de síntomas de estrés", "Alto", "TOTAL", "100 %",
                "3. RESULTADOS INTRALABORALES – FORMA A (3 PERSONAS)", "RANKING DE DOMINIOS", "Recompensas",
                "RANKING DE DIMENSIONES", "primero el de mayor porcentaje de personas en riesgo muy alto", "FORMA A – LIDERAZGO",
                "RECONOCIMIENTO Y COMPENSACIÓN", "Forma A · 3 personas", "DIMENSIÓN 5", "Forma A · 2 personas",
                "1 (50,0 %)", "Depende de una pregunta filtro")) {
            assertTrue(texto.contains(esperado), "falta: " + esperado);
        }
        assertFalse(texto.contains("muy alto +") || texto.contains("Alto + muy alto"), "no se suman niveles");
    }

}
