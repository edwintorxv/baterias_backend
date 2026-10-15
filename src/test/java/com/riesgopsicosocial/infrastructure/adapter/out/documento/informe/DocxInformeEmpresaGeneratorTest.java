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

    /** Dimensión de estrés con 10 respuestas entre Siempre y Nunca. */
    private static FrecuenciaDimension frecuencia(String nombre, String siempre, String nunca) {
        return new FrecuenciaDimension(1L, nombre, 5, 2, 10, List.of(
                new ConteoFrecuencia(new OpcionFrecuencia(1L, "Siempre"), 0, new BigDecimal(siempre)),
                new ConteoFrecuencia(new OpcionFrecuencia(5L, "Nunca"), 0, new BigDecimal(nunca))));
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
        InformeEmpresa informe = new InformeEmpresa(new DatosCliente(1L, "900", "Empresa X"), 2026, general,
                new ConcentracionRiesgo(List.of(new DominioConsolidado(1L, "Liderazgo", distribucion)),
                        List.of(new DistribucionGrupo("BOGOTA, D.C.", distribucion, distribucion), new DistribucionGrupo("CALI", distribucion, distribucion),
                                new DistribucionGrupo("MEDELLIN", distribucion, distribucion)),
                        List.of(new DistribucionGrupo("Ventas", distribucion, distribucion), new DistribucionGrupo("Operaciones", distribucion, distribucion),
                                new DistribucionGrupo("Dirección", distribucion, distribucion))),
                new SintomasEstres(3, List.of(
                        frecuencia("Síntomas psicoemocionales", "40.0", "60.0"),
                        frecuencia("Síntomas fisiológicos", "10.0", "90.0"))));

        byte[] word = new DocxInformeEmpresaGenerator().generar(informe, LocalDate.of(2026, 10, 13));

        String texto;
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(word));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            texto = extractor.getText();
            // Intralaboral, extralaboral, estrés, ranking de dominios y una ficha por dimensión (6).
            // Al final, la sección intralaboral con A y B juntas: dominios (1), por ciudad (2) y por área (2),
            // de a 2 grupos por gráfico; y la de estrés: frecuencia por dimensión (1), por ciudad (2) y por área (2).
            // La población por ciudad y el resumen de dimensiones no llevan gráfico.
            // getCharts() no garantiza el orden del documento: se identifican por sus categorías.
            assertEquals(20, doc.getCharts().size());
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

            assertTrue(categorias.contains(List.of("Liderazgo")), "dominios con A y B juntas");
            assertEquals(2, categorias.stream().filter(List.of("BOGOTA, D.C. (3)", "CALI (3)")::equals).count(),
                    "por ciudad, de a 2 y con su N: intralaboral y estrés");
            assertTrue(categorias.contains(List.of("MEDELLIN (3)")), "la ciudad que sobra va en su propio gráfico");
            assertTrue(categorias.contains(List.of("Ventas (3)", "Operaciones (3)")), "por área, de a 2 y con su N");
            assertTrue(categorias.contains(List.of("Dirección (3)")), "el área que sobra va en su propio gráfico");
            XWPFChart frecuencias = doc.getCharts().stream()
                    .filter(g -> categorias(g).equals(List.of("Síntomas psicoemocionales", "Síntomas fisiológicos")))
                    .findFirst().orElseThrow();
            assertEquals("Siempre", frecuencias.getCTChart().getPlotArea().getBarChartArray(0).getSerArray(0)
                    .getTx().getStrRef().getStrCache().getPtArray(0).getV(), "una barra por frecuencia");
            XWPFTable tablaFrecuencias = doc.getTables().stream()
                    .filter(t -> t.getRow(0).getCell(0).getText().equals("Dimensión")
                            && t.getRow(0).getCell(1).getText().equals("Respuestas (personas × ítems)"))
                    .findFirst().orElseThrow();
            assertEquals("10 (5 × 2)", tablaFrecuencias.getRow(1).getCell(1).getText(), "muestra de dónde sale la base");
            assertEquals("E53935", tablaFrecuencias.getRow(0).getCell(2).getColor(), "Siempre en rojo");
            assertEquals("00B050", tablaFrecuencias.getRow(0).getCell(3).getColor(), "Nunca en verde");
            List<String> titulos = doc.getCharts().stream()
                    .filter(g -> g.getTitle() != null)
                    .map(g -> g.getTitle().getBody().getParagraph(0).getText())
                    .toList();
            assertEquals(8, titulos.size(), "solo los gráficos por ciudad y por área llevan título");
            assertTrue(titulos.containsAll(List.of("Gráfico 1 de 2 – BOGOTA, D.C. y CALI", "Gráfico 2 de 2 – MEDELLIN",
                    "Gráfico 1 de 2 – Ventas y Operaciones", "Gráfico 2 de 2 – Dirección")), titulos.toString());
            XWPFChart dominios = doc.getCharts().stream()
                    .filter(g -> categorias(g).equals(List.of("Recompensas", "Liderazgo"))).findFirst().orElseThrow();
            assertEquals("0\" %\";;", dominios.getCTChart().getPlotArea().getBarChartArray(0).getSerArray(0)
                    .getDLbls().getNumFmt().getFormatCode(), "los 0 % no llevan etiqueta");
            XWPFTable porCiudad = doc.getTables().stream()
                    .filter(t -> t.getRow(0).getCell(0).getText().equals("Ciudad de trabajo")
                            && t.getRow(0).getTableCells().size() == 7)
                    .findFirst().orElseThrow();
            assertEquals(List.of("BOGOTA, D.C.", "3", "33,3 %", "66,7 %", "", "", ""),
                    porCiudad.getRow(1).getTableCells().stream().map(c -> c.getText()).toList());
            List<XWPFTable> tablasCiudad = doc.getTables().stream()
                    .filter(t -> t.getRow(0).getCell(0).getText().equals("Ciudad de trabajo")
                            && t.getRow(0).getTableCells().size() == 7)
                    .toList();
            assertEquals(2, tablasCiudad.size(), "intralaboral y estrés por ciudad");
            assertEquals(List.of("Riesgo bajo", "Riesgo alto"), List.of(tablasCiudad.get(0).getRow(0).getCell(2).getText(),
                    tablasCiudad.get(0).getRow(0).getCell(3).getText()));
            assertEquals(List.of("Bajo", "Alto"), List.of(tablasCiudad.get(1).getRow(0).getCell(2).getText(),
                    tablasCiudad.get(1).getRow(0).getCell(3).getText()), "estrés con las etiquetas del manual");
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
                "4. CONCENTRACIÓN DEL RIESGO INTRALABORAL (FORMAS A Y B)", "DOMINIOS INTRALABORALES",
                "POR CIUDAD DE TRABAJO (REGIONALES)", "POR ÁREA", "Nivel de riesgo intralaboral total, ordenado",
                "5. SÍNTOMAS DE ESTRÉS", "FRECUENCIA DE SÍNTOMAS POR DIMENSIÓN", "no trae baremos por dimensión", "no sobre las personas",
                "Nivel de síntomas de estrés, ordenado", "3. RESULTADOS INTRALABORALES – FORMA A (3 PERSONAS)", "RANKING DE DOMINIOS", "Recompensas",
                "RANKING DE DIMENSIONES", "primero el de mayor porcentaje de personas en riesgo muy alto", "FORMA A – LIDERAZGO",
                "RECONOCIMIENTO Y COMPENSACIÓN", "Forma A · 3 personas", "DIMENSIÓN 5", "Forma A · 2 personas",
                "1 (50,0 %)", "Depende de una pregunta filtro")) {
            assertTrue(texto.contains(esperado), "falta: " + esperado);
        }
        assertTrue(texto.indexOf("CONCENTRACIÓN DEL RIESGO") > texto.indexOf("FORMA A – LIDERAZGO"),
                "las secciones nuevas van al final, después de las fichas");
        assertTrue(texto.indexOf("5. SÍNTOMAS DE ESTRÉS") > texto.indexOf("4. CONCENTRACIÓN DEL RIESGO INTRALABORAL"),
                "estrés va en su propia sección, después de la intralaboral");
        assertTrue(texto.indexOf("6. CONCLUSIONES DEL PROFESIONAL") > texto.indexOf("5. SÍNTOMAS DE ESTRÉS")
                && texto.indexOf("7. PLAN DE ACCIÓN") > texto.indexOf("6. CONCLUSIONES DEL PROFESIONAL"),
                "conclusiones y plan de acción al final");
        for (String esperado : List.of("Escriba aquí las conclusiones del profesional.", "ACCIONES A CORTO PLAZO",
                "ACCIONES A MEDIANO PLAZO", "ACCIONES A LARGO PLAZO", "Escriba aquí las acciones a largo plazo.",
                "PROFESIONAL RESPONSABLE", "No. tarjeta profesional", "Firma del profesional")) {
            assertTrue(texto.contains(esperado), "falta: " + esperado);
        }
        assertTrue(texto.lastIndexOf("Firma del profesional") > texto.indexOf("ACCIONES A LARGO PLAZO"));
        assertFalse(texto.contains("muy alto +") || texto.contains("Alto + muy alto"), "no se suman niveles");
    }

}
