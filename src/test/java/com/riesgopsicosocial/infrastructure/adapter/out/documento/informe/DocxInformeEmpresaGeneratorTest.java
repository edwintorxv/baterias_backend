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
        DesgloseGrupos ciudades = new DesgloseGrupos(List.of(new DistribucionGrupo("BOGOTA, D.C.", distribucion),
                new DistribucionGrupo("CALI", distribucion), new DistribucionGrupo("Otras ciudades", distribucion)), 4, 0);
        DesgloseGrupos areas = new DesgloseGrupos(List.of(new DistribucionGrupo("Ventas", distribucion),
                new DistribucionGrupo("Operaciones", distribucion), new DistribucionGrupo("Dirección", distribucion)), 0, 1);
        DescripcionPoblacion poblacion = new DescripcionPoblacion(3, List.of(
                new VariablePoblacion("Forma intralaboral aplicada", List.of(new ConteoCategoria("Forma A", 3, new BigDecimal("100.0")))),
                new VariablePoblacion("Sexo", List.of(new ConteoCategoria("Femenino", 2, new BigDecimal("66.7")),
                        new ConteoCategoria("Masculino", 1, new BigDecimal("33.3")))),
                new VariablePoblacion("Rango de edad", List.of(new ConteoCategoria("25 a 34 años", 3, new BigDecimal("100.0"))))));
        InformeEmpresa informe = new InformeEmpresa(new DatosCliente(1L, "900", "Empresa X"), 2026, general, poblacion,
                new ConcentracionRiesgo(List.of(new DominioConsolidado(1L, "Liderazgo", distribucion)),
                        ciudades, areas, ciudades, new DesgloseGrupos(List.of(), 0, 1)),
                new ResultadosExtralaborales(List.of(
                        new DistribucionDimension(36L, "Tiempo fuera del trabajo", distribucion),
                        new DistribucionDimension(37L, "Relaciones familiares", distribucion),
                        new DistribucionDimension(38L, "Desplazamiento vivienda – trabajo – vivienda", distribucion)),
                        ciudades, areas),
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
            // de a 2 grupos por gráfico; y la de estrés: frecuencia por dimensión (1) y por ciudad (2). El estrés
            // por área no tiene grupos visibles (1 persona omitida): sin tabla ni gráfico, solo la nota.
            // Entre las dos, la extralaboral: dimensiones (2), por ciudad (2) y por área (2), de a 2.
            // La población por ciudad y el resumen de dimensiones no llevan gráfico.
            // getCharts() no garantiza el orden del documento: se identifican por sus categorías.
            assertEquals(24, doc.getCharts().size());
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
            assertEquals(3, categorias.stream().filter(List.of("BOGOTA, D.C. (3)", "CALI (3)")::equals).count(),
                    "por ciudad, de a 2 y con su N: intralaboral, extralaboral y estrés");
            assertTrue(categorias.contains(List.of("Otras ciudades (3)")), "la ciudad que sobra va en su propio gráfico");
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
            assertEquals(12, titulos.size(), "los gráficos de a 2 (ciudad, área, dimensiones extralaborales) llevan título");
            assertTrue(titulos.containsAll(List.of("Gráfico 1 de 2 – Tiempo fuera del trabajo y Relaciones familiares",
                    "Gráfico 2 de 2 – Desplazamiento vivienda – trabajo – vivienda")), titulos.toString());
            assertTrue(titulos.containsAll(List.of("Gráfico 1 de 2 – BOGOTA, D.C. y CALI", "Gráfico 2 de 2 – Otras ciudades",
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
            assertEquals(3, tablasCiudad.size(), "intralaboral, extralaboral y estrés por ciudad");
            assertEquals(List.of("Riesgo bajo", "Riesgo alto"), List.of(tablasCiudad.get(0).getRow(0).getCell(2).getText(),
                    tablasCiudad.get(0).getRow(0).getCell(3).getText()));
            assertEquals(List.of("Bajo", "Alto"), List.of(tablasCiudad.get(2).getRow(0).getCell(2).getText(),
                    tablasCiudad.get(2).getRow(0).getCell(3).getText()), "estrés con las etiquetas del manual");
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
                "5. CONCENTRACIÓN DEL RIESGO INTRALABORAL (FORMAS A Y B)", "DOMINIOS INTRALABORALES",
                "POR CIUDAD DE TRABAJO (REGIONALES)", "POR ÁREA", "Nivel de riesgo intralaboral total, ordenado",
                "7. SÍNTOMAS DE ESTRÉS", "FRECUENCIA DE SÍNTOMAS POR DIMENSIÓN", "no trae baremos por dimensión", "no sobre las personas",
                "\"Otras ciudades\" reúne 4 ciudades con menos de 2 personas evaluadas",
                "1 persona no se muestra para proteger la confidencialidad: pertenecen a áreas con menos de 2 personas",
                "Nivel de síntomas de estrés, ordenado", "4. RESULTADOS INTRALABORALES – FORMA A (3 PERSONAS)", "RANKING DE DOMINIOS", "Recompensas",
                "RANKING DE DIMENSIONES", "primero el de mayor porcentaje de personas en riesgo muy alto", "FORMA A – LIDERAZGO",
                "RECONOCIMIENTO Y COMPENSACIÓN", "Forma A · 3 personas", "DIMENSIÓN 5", "Forma A · 2 personas",
                "1 (50,0 %)", "Depende de una pregunta filtro")) {
            assertTrue(texto.contains(esperado), "falta: " + esperado);
        }
        assertTrue(texto.indexOf("CONCENTRACIÓN DEL RIESGO") > texto.indexOf("FORMA A – LIDERAZGO"),
                "las secciones nuevas van al final, después de las fichas");
        assertTrue(texto.indexOf("7. SÍNTOMAS DE ESTRÉS") > texto.indexOf("5. CONCENTRACIÓN DEL RIESGO INTRALABORAL"),
                "estrés va en su propia sección, después de la intralaboral");
        assertTrue(texto.indexOf("9. CONCLUSIONES DEL PROFESIONAL") > texto.indexOf("7. SÍNTOMAS DE ESTRÉS")
                && texto.indexOf("10. PLAN DE ACCIÓN") > texto.indexOf("9. CONCLUSIONES DEL PROFESIONAL"),
                "conclusiones y plan de acción al final");
        for (String esperado : List.of("Escriba aquí las conclusiones del profesional.", "ACCIONES A CORTO PLAZO",
                "ACCIONES A MEDIANO PLAZO", "ACCIONES A LARGO PLAZO", "Escriba aquí las acciones a largo plazo.",
                "PROFESIONAL RESPONSABLE", "No. tarjeta profesional", "Firma del profesional")) {
            assertTrue(texto.contains(esperado), "falta: " + esperado);
        }
        assertTrue(texto.lastIndexOf("Firma del profesional") > texto.indexOf("ACCIONES A LARGO PLAZO"));
        for (String esperado : List.of("1. INTRODUCCIÓN", "Resolución 2764 de 2022", "Empresa X realizó en 2026",
                "intralaboral forma A (jefes, profesionales y técnicos)", "cuestionario de factores de riesgo psicosocial extralaboral y "
                        + "cuestionario para la evaluación del estrés, tercera versión.",
                "4 personas esperadas, 3 evaluadas (75,0 %)", "menos de 2 personas evaluadas no se muestran por separado",
                "2. POBLACIÓN EVALUADA", "3. RESULTADOS GLOBALES", "8. INTERPRETACIÓN DE LOS NIVELES",
                "NIVELES DE RIESGO PSICOSOCIAL (INTRALABORAL Y EXTRALABORAL)", "requieren intervención inmediata", "Nivel de riesgo con amplia posibilidad",
                "NIVELES DE SÍNTOMAS DE ESTRÉS", "Muy bajo")) {
            assertTrue(texto.contains(esperado), "falta: " + esperado);
        }
        assertTrue(texto.contains("6. FACTORES DE RIESGO EXTRALABORALES") && texto.contains("DIMENSIONES EXTRALABORALES")
                && texto.contains("Nivel de riesgo extralaboral total, ordenado"), "sección extralaboral");
        assertTrue(texto.indexOf("6. FACTORES DE RIESGO EXTRALABORALES") > texto.indexOf("5. CONCENTRACIÓN DEL RIESGO INTRALABORAL")
                && texto.indexOf("6. FACTORES DE RIESGO EXTRALABORALES") < texto.indexOf("7. SÍNTOMAS DE ESTRÉS"),
                "el extralaboral va antes del estrés");
        assertTrue(texto.contains("CARACTERÍSTICAS DE LA POBLACIÓN EVALUADA") && texto.contains("Personas evaluadas (3)"));
        assertTrue(texto.indexOf("CARACTERÍSTICAS DE LA POBLACIÓN EVALUADA") > texto.indexOf("2. POBLACIÓN EVALUADA")
                && texto.indexOf("CARACTERÍSTICAS DE LA POBLACIÓN EVALUADA") < texto.indexOf("3. RESULTADOS GLOBALES"),
                "dentro de la sección de población");
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(word))) {
            List<XWPFTable> variables = doc.getTables().stream()
                    .filter(t -> t.getRow(0).getTableCells().size() == 7 && t.getRow(0).getCell(2).getText().equals("%"))
                    .toList();
            assertEquals(2, variables.size(), "tres variables: dos en la primera tabla y una en la segunda");
            XWPFTable primera = variables.get(0);
            assertEquals(List.of("Forma intralaboral aplicada", "Personas", "%", "", "Sexo", "Personas", "%"),
                    primera.getRow(0).getTableCells().stream().map(c -> c.getText()).toList());
            assertEquals(List.of("Forma A", "3", "100,0 %", "", "Femenino", "2", "66,7 %"),
                    primera.getRow(1).getTableCells().stream().map(c -> c.getText()).toList());
            assertEquals(List.of("", "", "", "", "Masculino", "1", "33,3 %"),
                    primera.getRow(2).getTableCells().stream().map(c -> c.getText()).toList(), "la variable más corta deja celdas vacías");
            assertEquals(List.of("TOTAL", "3", "100 %", "", "TOTAL", "3", "100 %"),
                    primera.getRow(3).getTableCells().stream().map(c -> c.getText()).toList(), "TOTAL alineado al final");
        }
        assertTrue(texto.indexOf("1. INTRODUCCIÓN") < texto.indexOf("2. POBLACIÓN EVALUADA"), "la introducción va al inicio");
        assertTrue(texto.indexOf("8. INTERPRETACIÓN DE LOS NIVELES") > texto.indexOf("7. SÍNTOMAS DE ESTRÉS")
                && texto.indexOf("8. INTERPRETACIÓN DE LOS NIVELES") < texto.indexOf("9. CONCLUSIONES DEL PROFESIONAL"),
                "la interpretación va antes de las conclusiones");
        assertFalse(texto.contains("muy alto +") || texto.contains("Alto + muy alto"), "no se suman niveles");
    }

}
