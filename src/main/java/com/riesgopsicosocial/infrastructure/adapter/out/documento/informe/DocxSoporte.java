package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import java.util.Map;

/**
 * Piezas comunes de los informes en Word: página carta, tablas de ancho fijo, barras de
 * sección, textos y numeración "Página X de Y".
 */
final class DocxSoporte {

    static final String FUENTE = "Arial";
    static final String AZUL = "1F3B57";
    static final String GRIS_ETIQUETA = "F3F5F7";
    static final String GRIS_ENCABEZADO = "E7ECF1";
    static final String GRIS_TOTAL_GENERAL = "DFE6ED";
    static final String BORDE = "9AA5B1";
    /** Semáforo por nivel de riesgo (1 = sin riesgo … 5 = muy alto), igual en tablas y gráficos. */
    static final Map<Long, String> COLOR_NIVEL = Map.of(
            1L, "00B050", 2L, "92D050", 3L, "FFD700", 4L, "FF8C00", 5L, "E53935");
    /** Ancho útil de la página carta con márgenes de 1,8 cm, en twips (1/20 de punto). */
    static final int ANCHO_TEXTO = 10200;

    private DocxSoporte() {
    }

    /** Tabla de ancho fijo (en twips): los visores que no entienden porcentajes la muestran igual. */
    static XWPFTable tabla(XWPFDocument doc, int filas, int[] anchos) {
        XWPFTable tabla = doc.createTable(filas, anchos.length);
        CTTblPr propiedades = tabla.getCTTbl().getTblPr();
        CTTblWidth ancho = propiedades.isSetTblW() ? propiedades.getTblW() : propiedades.addNewTblW();
        ancho.setType(STTblWidth.DXA);
        ancho.setW(BigInteger.valueOf(ANCHO_TEXTO));
        (propiedades.isSetTblLayout() ? propiedades.getTblLayout() : propiedades.addNewTblLayout())
                .setType(STTblLayoutType.FIXED);
        CTTblGrid grilla = tabla.getCTTbl().getTblGrid() == null
                ? tabla.getCTTbl().addNewTblGrid() : tabla.getCTTbl().getTblGrid();
        while (grilla.sizeOfGridColArray() > 0) {
            grilla.removeGridCol(0);
        }
        for (int valor : anchos) {
            grilla.addNewGridCol().setW(BigInteger.valueOf(valor));
        }
        tabla.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDE);
        tabla.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDE);
        tabla.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDE);
        tabla.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDE);
        tabla.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDE);
        tabla.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, BORDE);
        tabla.setCellMargins(40, 100, 40, 100);
        return tabla;
    }

    static void celda(XWPFTableCell celda, String valor, boolean negrilla, ParagraphAlignment alineacion,
                      String color, int ancho) {
        XWPFParagraph parrafo = celda.getParagraphs().get(0);
        parrafo.setAlignment(alineacion);
        parrafo.setSpacingAfter(0);
        texto(parrafo, valor == null ? "" : valor, 9, negrilla, null);
        celda.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        if (color != null) {
            celda.setColor(color);
        }
        CTTcPr propiedades = propiedades(celda);
        CTTblWidth anchoCelda = propiedades.isSetTcW() ? propiedades.getTcW() : propiedades.addNewTcW();
        anchoCelda.setType(STTblWidth.DXA);
        anchoCelda.setW(BigInteger.valueOf(ancho));
    }

    static void encabezado(XWPFTableRow fila, int[] anchos, String... titulos) {
        for (int i = 0; i < titulos.length; i++) {
            celda(fila.getCell(i), titulos[i], true, ParagraphAlignment.CENTER, GRIS_ENCABEZADO, anchos[i]);
        }
        fila.setRepeatHeader(true);
    }

    static CTTcPr propiedades(XWPFTableCell celda) {
        CTTc ctTc = celda.getCTTc();
        return ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
    }

    /** Barra azul de sección: una tabla de una celda, que todos los visores muestran con su fondo. */
    static void seccion(XWPFDocument doc, String titulo) {
        parrafo(doc, ParagraphAlignment.LEFT, 120, 0);
        XWPFTable tabla = tabla(doc, 1, new int[]{ANCHO_TEXTO});
        XWPFTableCell celda = tabla.getRow(0).getCell(0);
        XWPFParagraph parrafo = celda.getParagraphs().get(0);
        parrafo.setSpacingAfter(0);
        parrafo.setKeepNext(true);
        texto(parrafo, titulo, 10.5, true, "FFFFFF");
        celda.setColor(AZUL);
        celda.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        CTTblWidth anchoCelda = propiedades(celda).addNewTcW();
        anchoCelda.setType(STTblWidth.DXA);
        anchoCelda.setW(BigInteger.valueOf(ANCHO_TEXTO));
        espacio(doc);
    }

    static void subtitulo(XWPFDocument doc, String titulo) {
        XWPFParagraph parrafo = parrafo(doc, ParagraphAlignment.LEFT, 200, 80);
        parrafo.setKeepNext(true);
        texto(parrafo, titulo.toUpperCase(), 10, true, AZUL);
    }

    static XWPFParagraph parrafo(XWPFDocument doc, ParagraphAlignment alineacion, int antes, int despues) {
        XWPFParagraph parrafo = doc.createParagraph();
        parrafo.setAlignment(alineacion);
        parrafo.setSpacingBefore(antes);
        parrafo.setSpacingAfter(despues);
        return parrafo;
    }

    static XWPFRun texto(XWPFParagraph parrafo, String valor, double tamano, boolean negrilla, String color) {
        XWPFRun run = parrafo.createRun();
        run.setFontFamily(FUENTE);
        run.setFontSize(tamano);
        run.setBold(negrilla);
        if (color != null) {
            run.setColor(color);
        }
        run.setText(valor);
        return run;
    }

    static void espacio(XWPFDocument doc) {
        parrafo(doc, ParagraphAlignment.LEFT, 0, 60);
    }

    static void saltoDePagina(XWPFDocument doc) {
        doc.createParagraph().setPageBreak(true);
    }

    /** Carta, márgenes como el PDF, encabezado con la nota de confidencialidad y "Página X de Y". */
    static void configurarPagina(XWPFDocument doc, String notaEncabezado) {
        CTSectPr seccion = doc.getDocument().getBody().isSetSectPr()
                ? doc.getDocument().getBody().getSectPr() : doc.getDocument().getBody().addNewSectPr();
        CTPageSz tamano = seccion.isSetPgSz() ? seccion.getPgSz() : seccion.addNewPgSz();
        tamano.setW(BigInteger.valueOf(12240));
        tamano.setH(BigInteger.valueOf(15840));
        CTPageMar margenes = seccion.isSetPgMar() ? seccion.getPgMar() : seccion.addNewPgMar();
        margenes.setTop(BigInteger.valueOf(1134));
        margenes.setBottom(BigInteger.valueOf(1134));
        margenes.setLeft(BigInteger.valueOf(1020));
        margenes.setRight(BigInteger.valueOf(1020));
        margenes.setHeader(BigInteger.valueOf(500));
        margenes.setFooter(BigInteger.valueOf(500));

        XWPFHeader encabezado = doc.createHeader(HeaderFooterType.DEFAULT);
        XWPFParagraph textoEncabezado = encabezado.createParagraph();
        textoEncabezado.setAlignment(ParagraphAlignment.CENTER);
        texto(textoEncabezado, notaEncabezado, 7.5, false, "8A1C1C");

        XWPFFooter pie = doc.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph textoPie = pie.createParagraph();
        textoPie.setAlignment(ParagraphAlignment.CENTER);
        texto(textoPie, "Página ", 8, false, "555555");
        campo(textoPie, "PAGE");
        texto(textoPie, " de ", 8, false, "555555");
        campo(textoPie, "NUMPAGES");
    }

    /** Campo de Word (PAGE, NUMPAGES) que se actualiza al abrir el documento. */
    private static void campo(XWPFParagraph parrafo, String instruccion) {
        XWPFRun inicio = texto(parrafo, "", 8, false, "555555");
        inicio.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        XWPFRun codigo = texto(parrafo, "", 8, false, "555555");
        CTText instr = codigo.getCTR().addNewInstrText();
        instr.setStringValue(" " + instruccion + " ");
        XWPFRun separador = texto(parrafo, "", 8, false, "555555");
        separador.getCTR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        texto(parrafo, "1", 8, false, "555555");
        XWPFRun fin = texto(parrafo, "", 8, false, "555555");
        fin.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
    }

}
