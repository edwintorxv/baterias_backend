package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xddf.usermodel.text.XDDFRunProperties;
import org.apache.poi.xddf.usermodel.text.XDDFTextParagraph;
import org.apache.poi.xddf.usermodel.text.XDDFTextRun;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFChart;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static com.riesgopsicosocial.infrastructure.adapter.out.documento.informe.DocxSoporte.*;

/**
 * Gráficos de barras nativos de Word (con sus datos en el libro de Excel incrustado, así que
 * se pueden editar desde Word).
 */
final class DocxGraficos {

    /** Ancho del texto (twips) en EMU: 1 twip = 635 EMU. */
    private static final int ANCHO = ANCHO_TEXTO * 635;
    /** Unos 6,5 cm de alto. */
    private static final int ALTO = 2_340_000;
    /** Unos 8 cm: los gráficos agrupados llevan leyenda y nombres largos en el eje X. */
    static final int ALTO_AGRUPADO = 2_880_000;
    /** Unos 6 cm: gráficos de pocos grupos (ciudades, áreas), que se repiten varios seguidos. */
    static final int ALTO_COMPACTO = 2_160_000;

    private DocxGraficos() {
    }

    /** Una serie de barras: nombre (leyenda), porcentajes por categoría y color. */
    record Serie(String nombre, Double[] porcentajes, String color) {
    }

    /** Barras verticales de porcentajes (eje 0–100 %), cada barra con su color y su valor encima. */
    static void barras(XWPFDocument doc, String[] categorias, Double[] porcentajes, List<String> colores) {
        XWPFChart grafico = grafico(doc, ALTO);
        XDDFBarChartData datos = datos(grafico, 60);
        XDDFBarChartData.Series serie = agregarSerie(grafico, datos, categorias,
                new Serie("Porcentaje de personas", porcentajes, null), 1);
        etiquetasDeValor(serie, "0.0\" %\"", null);
        for (int i = 0; i < colores.size(); i++) {
            XDDFDataPoint punto = serie.getDataPoint(i);
            punto.setFillProperties(relleno(colores.get(i)));
            punto.setLineProperties(linea(BORDE));
        }
        grafico.plot(datos);
    }

    static void barrasAgrupadas(XWPFDocument doc, String[] categorias, List<Serie> series) {
        barrasAgrupadas(doc, categorias, series, null, ALTO_AGRUPADO);
    }

    /**
     * Barras agrupadas: por cada categoría, una barra por serie lado a lado (p. ej. un grupo por
     * dominio con una barra por nivel), con leyenda abajo. Los valores van sin decimales y en letra
     * pequeña para que quepan, y los 0 % no se rotulan; el detalle queda en la tabla.
     *
     * @param titulo título dentro del gráfico (editable en Word y se copia con él); null = sin título
     * @param alto   alto en EMU ({@link #ALTO_AGRUPADO}, {@link #ALTO_COMPACTO})
     */
    static void barrasAgrupadas(XWPFDocument doc, String[] categorias, List<Serie> series, String titulo, int alto) {
        XWPFChart grafico = grafico(doc, alto);
        if (titulo != null) {
            titulo(grafico, titulo);
        }
        XDDFBarChartData datos = datos(grafico, 80);
        for (int i = 0; i < series.size(); i++) {
            XDDFBarChartData.Series barras = agregarSerie(grafico, datos, categorias, series.get(i), i + 1);
            barras.setFillProperties(relleno(series.get(i).color()));
            barras.setLineProperties(linea(BORDE));
            // Formato con secciones vacías para cero y negativos: los 0 % no llevan etiqueta.
            etiquetasDeValor(barras, "0\" %\";;", 700);
        }
        grafico.plot(datos);
        grafico.getOrAddLegend().setPosition(LegendPosition.BOTTOM);
    }

    private static XWPFChart grafico(XWPFDocument doc, int alto) {
        try {
            XWPFParagraph parrafo = parrafo(doc, ParagraphAlignment.CENTER, 0, 60);
            XWPFChart grafico = doc.createChart(parrafo.createRun(), ANCHO, alto);
            grafico.setAutoTitleDeleted(true);
            return grafico;
        } catch (IOException | InvalidFormatException e) {
            throw new UncheckedIOException("No se pudo crear el gráfico del informe",
                    e instanceof IOException io ? io : new IOException(e));
        }
    }

    /** Título de 10 pt en negrilla, arriba y sin montarse sobre las barras. */
    private static void titulo(XWPFChart grafico, String titulo) {
        grafico.setAutoTitleDeleted(false);
        grafico.setTitleText(titulo);
        grafico.setTitleOverlay(false);
        XDDFTextParagraph parrafo = grafico.getTitle().getBody().getParagraph(0);
        XDDFRunProperties formato = parrafo.addDefaultRunProperties();
        formato.setFontSize(10.0);
        formato.setBold(true);
        for (XDDFTextRun run : parrafo.getTextRuns()) {
            run.setFontSize(10.0);
            run.setBold(true);
        }
    }

    /** Ejes (Y de 0 a 100 %) y barras verticales. */
    private static XDDFBarChartData datos(XWPFChart grafico, int separacion) {
        XDDFCategoryAxis ejeCategorias = grafico.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis ejeValores = grafico.createValueAxis(AxisPosition.LEFT);
        ejeValores.setCrosses(AxisCrosses.AUTO_ZERO);
        // Cada categoría en su propio espacio: sin esto la primera barra queda montada sobre el eje Y.
        ejeValores.setCrossBetween(AxisCrossBetween.BETWEEN);
        ejeValores.setMinimum(0);
        ejeValores.setMaximum(100);
        ejeValores.setMajorUnit(20);
        ejeValores.setNumberFormat("0\" %\"");
        ejeValores.getOrAddMajorGridProperties().setLineProperties(linea("D9D9D9"));

        XDDFBarChartData datos = (XDDFBarChartData) grafico.createData(ChartTypes.BAR, ejeCategorias, ejeValores);
        datos.setBarDirection(BarDirection.COL);
        datos.setBarGrouping(BarGrouping.CLUSTERED);
        datos.setGapWidth(separacion);
        datos.setVaryColors(false);
        return datos;
    }

    /** Agrega la serie con sus datos en la columna {@code columna} del Excel incrustado (la 0 son las categorías). */
    private static XDDFBarChartData.Series agregarSerie(XWPFChart grafico, XDDFBarChartData datos, String[] categorias,
                                                       Serie serie, int columna) {
        int ultimaFila = categorias.length;
        XDDFDataSource<String> fuenteCategorias = XDDFDataSourcesFactory.fromArray(categorias,
                grafico.formatRange(new CellRangeAddress(1, ultimaFila, 0, 0)), 0);
        XDDFNumericalDataSource<Double> fuenteValores = XDDFDataSourcesFactory.fromArray(serie.porcentajes(),
                grafico.formatRange(new CellRangeAddress(1, ultimaFila, columna, columna)), columna);
        XDDFBarChartData.Series barras = (XDDFBarChartData.Series) datos.addSeries(fuenteCategorias, fuenteValores);
        barras.setTitle(serie.nombre(), grafico.setSheetTitle(serie.nombre(), columna));
        return barras;
    }

    /**
     * Muestra el porcentaje encima de cada barra.
     *
     * @param tamano tamaño de letra en centésimas de punto (700 = 7 pt); null = el de Word
     */
    private static void etiquetasDeValor(XDDFBarChartData.Series serie, String formato, Integer tamano) {
        CTDLbls etiquetas = serie.getCTBarSer().isSetDLbls()
                ? serie.getCTBarSer().getDLbls() : serie.getCTBarSer().addNewDLbls();
        etiquetas.addNewNumFmt().setFormatCode(formato);
        etiquetas.getNumFmt().setSourceLinked(false);
        if (tamano != null) {
            CTTextBody texto = etiquetas.addNewTxPr();
            texto.addNewBodyPr();
            texto.addNewLstStyle();
            texto.addNewP().addNewPPr().addNewDefRPr().setSz(tamano);
        }
        etiquetas.addNewShowLegendKey().setVal(false);
        etiquetas.addNewShowVal().setVal(true);
        etiquetas.addNewShowCatName().setVal(false);
        etiquetas.addNewShowSerName().setVal(false);
        etiquetas.addNewShowPercent().setVal(false);
        etiquetas.addNewShowBubbleSize().setVal(false);
    }

    private static XDDFSolidFillProperties relleno(String colorHex) {
        return new XDDFSolidFillProperties(XDDFColor.from(new byte[]{
                (byte) Integer.parseInt(colorHex.substring(0, 2), 16),
                (byte) Integer.parseInt(colorHex.substring(2, 4), 16),
                (byte) Integer.parseInt(colorHex.substring(4, 6), 16)}));
    }

    private static XDDFLineProperties linea(String colorHex) {
        XDDFLineProperties linea = new XDDFLineProperties(relleno(colorHex));
        linea.setWidth(0.75);
        return linea;
    }

}
