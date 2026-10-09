package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import com.riesgopsicosocial.application.port.out.informe.GeneradorDocumentoInformePort;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.domain.model.informe.InformeEvaluado;
import com.riesgopsicosocial.infrastructure.adapter.out.documento.informe.InformeVista.*;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Informe individual en Word, con el mismo contenido que el PDF ({@link InformeVista}).
 * Queda editable para que el psicólogo pueda ajustar observaciones y recomendaciones.
 */
@Component
public class DocxInformeEvaluadoGenerator implements GeneradorDocumentoInformePort {

    private static final String FUENTE = "Arial";
    private static final String AZUL = "1F3B57";
    private static final String GRIS_ETIQUETA = "F3F5F7";
    private static final String GRIS_ENCABEZADO = "E7ECF1";
    private static final String GRIS_TOTAL = "EEF1F4";
    private static final String GRIS_TOTAL_GENERAL = "DFE6ED";
    private static final String BORDE = "9AA5B1";
    private static final Map<Long, String> COLOR_NIVEL = Map.of(
            1L, "D9F0D3", 2L, "EEF6C9", 3L, "FFF2B3", 4L, "FFD2A8", 5L, "F7B4B0");
    /** Ancho útil de la página carta con márgenes de 1,8 cm, en twips (1/20 de punto). */
    private static final int ANCHO_TEXTO = 10200;
    private static final int[] ANCHOS_DATOS = {3876, 6324};
    private static final int[] ANCHOS_INTRALABORAL = {2244, 4182, 1326, 2448};
    private static final int[] ANCHOS_TRES_COLUMNAS = {6426, 1326, 2448};
    private static final double ANCHO_MAXIMO_FIRMA = 200;
    private static final double ALTO_MAXIMO_FIRMA = 60;

    private final InformeVistaMapper vistaMapper;

    public DocxInformeEvaluadoGenerator(InformeVistaMapper vistaMapper) {
        this.vistaMapper = vistaMapper;
    }

    @Override
    public FormatoDocumento formato() {
        return FormatoDocumento.DOCX;
    }

    @Override
    public byte[] generar(InformeEvaluado informe, Map<Long, FirmaEvaluador> firmas, LocalDate fechaElaboracion) {
        InformeVista vista = vistaMapper.toVista(informe, firmas, fechaElaboracion);
        try (XWPFDocument documento = new XWPFDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            configurarPagina(documento);
            for (int i = 0; i < vista.aplicaciones().size(); i++) {
                if (i > 0) {
                    saltoDePagina(documento);
                }
                escribirAplicacion(documento, vista, vista.aplicaciones().get(i));
            }
            documento.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Word del informe", e);
        }
    }

    // ---------------------------------------------------------------- estructura

    private void escribirAplicacion(XWPFDocument doc, InformeVista vista, AplicacionVista aplicacion) {
        XWPFParagraph titulo = parrafo(doc, ParagraphAlignment.CENTER, 0, 60);
        texto(titulo, "INFORME DE RESULTADOS DE LA BATERÍA DE INSTRUMENTOS PARA LA EVALUACIÓN DE FACTORES DE RIESGO PSICOSOCIAL",
                12.5, true, null);
        texto(parrafo(doc, ParagraphAlignment.CENTER, 0, 200),
                "Informe individual – Ministerio de la Protección Social, Pontificia Universidad Javeriana (2010)", 9, false, "444444");

        seccion(doc, "DATOS GENERALES DEL TRABAJADOR");
        tablaDatos(doc, List.of(
                new String[]{"Nombre del trabajador", vista.nombreTrabajador()},
                new String[]{"Número de identificación (ID)", vista.identificacion()},
                new String[]{"Cargo", aplicacion.cargo()},
                new String[]{"Departamento o sección", aplicacion.area()},
                new String[]{"Edad", aplicacion.edad()},
                new String[]{"Sexo", vista.sexo()},
                new String[]{"Fecha de aplicación del cuestionario", aplicacion.fechaAplicacion()},
                new String[]{"Nombre de la empresa", vista.empresa() + " (NIT " + vista.nitEmpresa() + ")"},
                new String[]{"Grupo ocupacional (baremos)", aplicacion.grupoOcupacional()}));

        EvaluadorVista evaluador = aplicacion.evaluador();
        seccion(doc, "DATOS DEL EVALUADOR");
        tablaDatos(doc, List.of(
                new String[]{"Nombre del evaluador", evaluador.nombre()},
                new String[]{"Número de identificación (c.c.)", evaluador.identificacion()},
                new String[]{"Profesión", evaluador.profesion()},
                new String[]{"Posgrado", evaluador.posgrado()},
                new String[]{"No. tarjeta profesional", evaluador.tarjetaProfesional()},
                new String[]{"No. licencia en salud ocupacional", evaluador.licenciaSaludOcupacional()},
                new String[]{"Fecha de expedición de la licencia en salud ocupacional", evaluador.fechaExpedicionLicencia()}));

        saltoDePagina(doc);
        seccion(doc, "RESULTADOS DE LOS CUESTIONARIOS");
        for (CuestionarioVista cuestionario : aplicacion.cuestionarios()) {
            subtitulo(doc, cuestionario.titulo());
            switch (cuestionario.tipo()) {
                case INTRALABORAL -> tablaIntralaboral(doc, cuestionario);
                case EXTRALABORAL -> tablaFilas(doc, "Dimensiones", cuestionario.tituloNivel(),
                        cuestionario.dimensiones(), cuestionario.total(), true);
                case ESTRES -> tablaFilas(doc, "", cuestionario.tituloNivel(), List.of(), cuestionario.total(), true);
            }
        }
        if (aplicacion.totalGeneral() != null) {
            subtitulo(doc, "Puntaje total general de factores de riesgo psicosocial");
            tablaFilas(doc, "", "Nivel de riesgo", List.of(), aplicacion.totalGeneral(), false);
        }

        seccion(doc, "INTERPRETACIÓN GENÉRICA DE LOS NIVELES DE RIESGO");
        vista.interpretacionRiesgo().forEach(p -> interpretacion(doc, p));
        if (aplicacion.incluyeEstres()) {
            seccion(doc, "INTERPRETACIÓN DE LOS NIVELES DE SÍNTOMAS DE ESTRÉS");
            vista.interpretacionEstres().forEach(p -> interpretacion(doc, p));
        }

        seccion(doc, "OBSERVACIONES Y COMENTARIOS DEL EVALUADOR");
        cuadroTexto(doc, aplicacion.observaciones());
        seccion(doc, "RECOMENDACIONES PARTICULARES");
        cuadroTexto(doc, aplicacion.recomendaciones());

        XWPFParagraph fecha = parrafo(doc, ParagraphAlignment.LEFT, 200, 300);
        texto(fecha, "Fecha de elaboración del informe: ", 9.5, false, null);
        texto(fecha, vista.fechaElaboracion(), 9.5, true, null);

        firma(doc, evaluador);
    }

    // ---------------------------------------------------------------- tablas

    private void tablaDatos(XWPFDocument doc, List<String[]> filas) {
        XWPFTable tabla = tabla(doc, filas.size(), ANCHOS_DATOS);
        for (int i = 0; i < filas.size(); i++) {
            XWPFTableRow fila = tabla.getRow(i);
            celda(fila.getCell(0), filas.get(i)[0], true, ParagraphAlignment.LEFT, GRIS_ETIQUETA, ANCHOS_DATOS[0]);
            celda(fila.getCell(1), filas.get(i)[1], false, ParagraphAlignment.LEFT, null, ANCHOS_DATOS[1]);
        }
        espacio(doc);
    }

    /** Dominios con sus dimensiones; la celda del dominio ocupa todas sus filas, como en el manual. */
    private void tablaIntralaboral(XWPFDocument doc, CuestionarioVista cuestionario) {
        XWPFTable tabla = tabla(doc, 1, ANCHOS_INTRALABORAL);
        encabezado(tabla.getRow(0), ANCHOS_INTRALABORAL, "Dominios", "Dimensiones", "Puntaje (transformado)",
                cuestionario.tituloNivel());
        int[] anchos = ANCHOS_INTRALABORAL;
        for (DominioVista dominio : cuestionario.dominios()) {
            for (int i = 0; i < dominio.dimensiones().size(); i++) {
                FilaVista dimension = dominio.dimensiones().get(i);
                XWPFTableRow fila = tabla.createRow();
                celda(fila.getCell(0), i == 0 ? dominio.nombre() : "", true, ParagraphAlignment.LEFT, GRIS_ETIQUETA, anchos[0]);
                combinarVertical(fila.getCell(0), i == 0);
                celda(fila.getCell(1), dimension.nombre(), false, ParagraphAlignment.LEFT, null, anchos[1]);
                celda(fila.getCell(2), dimension.puntaje(), false, ParagraphAlignment.CENTER, null, anchos[2]);
                celda(fila.getCell(3), dimension.nivel(), false, ParagraphAlignment.CENTER, COLOR_NIVEL.get(dimension.idNivel()), anchos[3]);
            }
            filaTotal(tabla, dominio.total(), GRIS_TOTAL, false);
        }
        filaTotal(tabla, cuestionario.total(), GRIS_TOTAL_GENERAL, true);
        espacio(doc);
    }

    /** Extralaboral (dimensiones + total), estrés y total general (solo total). */
    private void tablaFilas(XWPFDocument doc, String tituloNombre, String tituloNivel, List<FilaVista> filas,
                            FilaVista total, boolean totalEnMayusculas) {
        XWPFTable tabla = tabla(doc, 1, ANCHOS_TRES_COLUMNAS);
        encabezado(tabla.getRow(0), ANCHOS_TRES_COLUMNAS, tituloNombre, "Puntaje (transformado)", tituloNivel);
        int[] anchos = ANCHOS_TRES_COLUMNAS;
        for (FilaVista dimension : filas) {
            XWPFTableRow fila = tabla.createRow();
            celda(fila.getCell(0), dimension.nombre(), false, ParagraphAlignment.LEFT, null, anchos[0]);
            celda(fila.getCell(1), dimension.puntaje(), false, ParagraphAlignment.CENTER, null, anchos[1]);
            celda(fila.getCell(2), dimension.nivel(), false, ParagraphAlignment.CENTER, COLOR_NIVEL.get(dimension.idNivel()), anchos[2]);
        }
        XWPFTableRow fila = tabla.createRow();
        String nombre = totalEnMayusculas ? total.nombre().toUpperCase() : total.nombre();
        celda(fila.getCell(0), nombre, true, ParagraphAlignment.LEFT, GRIS_TOTAL_GENERAL, anchos[0]);
        celda(fila.getCell(1), total.puntaje(), true, ParagraphAlignment.CENTER, GRIS_TOTAL_GENERAL, anchos[1]);
        celda(fila.getCell(2), total.nivel(), true, ParagraphAlignment.CENTER, COLOR_NIVEL.get(total.idNivel()), anchos[2]);
        espacio(doc);
    }

    /** Fila de total de una tabla de 4 columnas: nombre en las dos primeras celdas combinadas. */
    private void filaTotal(XWPFTable tabla, FilaVista total, String color, boolean enMayusculas) {
        XWPFTableRow fila = tabla.createRow();
        fila.removeCell(1);
        CTTcPr propiedades = propiedades(fila.getCell(0));
        propiedades.addNewGridSpan().setVal(BigInteger.valueOf(2));
        celda(fila.getCell(0), enMayusculas ? total.nombre().toUpperCase() : total.nombre(), true,
                ParagraphAlignment.LEFT, color, ANCHOS_INTRALABORAL[0] + ANCHOS_INTRALABORAL[1]);
        celda(fila.getCell(1), total.puntaje(), true, ParagraphAlignment.CENTER, color, ANCHOS_INTRALABORAL[2]);
        celda(fila.getCell(2), total.nivel(), true, ParagraphAlignment.CENTER, COLOR_NIVEL.get(total.idNivel()),
                ANCHOS_INTRALABORAL[3]);
    }

    private void encabezado(XWPFTableRow fila, int[] anchos, String... titulos) {
        for (int i = 0; i < titulos.length; i++) {
            celda(fila.getCell(i), titulos[i], true, ParagraphAlignment.CENTER, GRIS_ENCABEZADO, anchos[i]);
        }
        fila.setRepeatHeader(true);
    }

    /** Tabla de ancho fijo (en twips): los visores que no entienden porcentajes la muestran igual. */
    private XWPFTable tabla(XWPFDocument doc, int filas, int[] anchos) {
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

    private void celda(XWPFTableCell celda, String valor, boolean negrilla, ParagraphAlignment alineacion,
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

    private void combinarVertical(XWPFTableCell celda, boolean inicio) {
        propiedades(celda).addNewVMerge().setVal(inicio ? STMerge.RESTART : STMerge.CONTINUE);
    }

    private CTTcPr propiedades(XWPFTableCell celda) {
        CTTc ctTc = celda.getCTTc();
        return ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
    }

    // ---------------------------------------------------------------- textos

    /** Barra azul de sección: una tabla de una celda, que todos los visores muestran con su fondo. */
    private void seccion(XWPFDocument doc, String titulo) {
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

    private void subtitulo(XWPFDocument doc, String titulo) {
        XWPFParagraph parrafo = parrafo(doc, ParagraphAlignment.LEFT, 200, 80);
        parrafo.setKeepNext(true);
        texto(parrafo, titulo.toUpperCase(), 10, true, AZUL);
    }

    private void interpretacion(XWPFDocument doc, ParrafoVista p) {
        XWPFParagraph parrafo = parrafo(doc, ParagraphAlignment.BOTH, 0, 60);
        texto(parrafo, p.titulo() + ": ", 8.5, true, null);
        texto(parrafo, p.texto(), 8.5, false, null);
    }

    /** Recuadro editable con saltos de línea respetados. */
    private void cuadroTexto(XWPFDocument doc, String contenido) {
        XWPFTable tabla = tabla(doc, 1, new int[]{ANCHO_TEXTO});
        XWPFTableCell celda = tabla.getRow(0).getCell(0);
        CTTblWidth anchoCelda = propiedades(celda).addNewTcW();
        anchoCelda.setType(STTblWidth.DXA);
        anchoCelda.setW(BigInteger.valueOf(ANCHO_TEXTO));
        XWPFParagraph parrafo = celda.getParagraphs().get(0);
        String[] lineas = contenido == null ? new String[]{""} : contenido.split("\\R", -1);
        for (int i = 0; i < lineas.length; i++) {
            XWPFRun run = texto(parrafo, lineas[i], 9.5, false, null);
            if (i < lineas.length - 1) {
                run.addBreak();
            }
        }
        tabla.getRow(0).setHeight(900);
        espacio(doc);
    }

    private void firma(XWPFDocument doc, EvaluadorVista evaluador) {
        if (evaluador.firma() != null) {
            XWPFParagraph imagen = parrafo(doc, ParagraphAlignment.LEFT, 300, 0);
            try {
                BufferedImage leida = ImageIO.read(new ByteArrayInputStream(evaluador.firma()));
                double escala = leida == null ? 1
                        : Math.min(1, Math.min(ANCHO_MAXIMO_FIRMA / leida.getWidth(), ALTO_MAXIMO_FIRMA / leida.getHeight()));
                double ancho = leida == null ? ANCHO_MAXIMO_FIRMA : leida.getWidth() * escala;
                double alto = leida == null ? ALTO_MAXIMO_FIRMA : leida.getHeight() * escala;
                int tipo = "image/jpeg".equals(evaluador.firmaTipoContenido())
                        ? Document.PICTURE_TYPE_JPEG : Document.PICTURE_TYPE_PNG;
                imagen.createRun().addPicture(new ByteArrayInputStream(evaluador.firma()), tipo, "firma",
                        Units.toEMU(ancho), Units.toEMU(alto));
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo insertar la firma del evaluador en el Word", e);
            }
        }
        XWPFParagraph linea = parrafo(doc, ParagraphAlignment.LEFT, evaluador.firma() == null ? 600 : 0, 0);
        linea.setBorderTop(Borders.SINGLE);
        linea.setIndentationRight(5500);
        texto(linea, "Firma del evaluador", 9.5, false, null);
        texto(parrafo(doc, ParagraphAlignment.LEFT, 0, 0), evaluador.nombre(), 9.5, false, null);
        texto(parrafo(doc, ParagraphAlignment.LEFT, 0, 0),
                "T.P. " + evaluador.tarjetaProfesional() + " – Licencia SO " + evaluador.licenciaSaludOcupacional(), 9.5, false, null);
    }

    private XWPFParagraph parrafo(XWPFDocument doc, ParagraphAlignment alineacion, int antes, int despues) {
        XWPFParagraph parrafo = doc.createParagraph();
        parrafo.setAlignment(alineacion);
        parrafo.setSpacingBefore(antes);
        parrafo.setSpacingAfter(despues);
        return parrafo;
    }

    private XWPFRun texto(XWPFParagraph parrafo, String valor, double tamano, boolean negrilla, String color) {
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

    private void espacio(XWPFDocument doc) {
        parrafo(doc, ParagraphAlignment.LEFT, 0, 60);
    }

    private void saltoDePagina(XWPFDocument doc) {
        doc.createParagraph().setPageBreak(true);
    }

    // ---------------------------------------------------------------- página

    /** Carta, márgenes como el PDF, encabezado de confidencialidad y "Página X de Y". */
    private void configurarPagina(XWPFDocument doc) {
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
        texto(textoEncabezado, "CONFIDENCIAL – Hace parte de la historia clínica ocupacional del trabajador", 7.5, false, "8A1C1C");

        XWPFFooter pie = doc.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph textoPie = pie.createParagraph();
        textoPie.setAlignment(ParagraphAlignment.CENTER);
        texto(textoPie, "Página ", 8, false, "555555");
        campo(textoPie, "PAGE");
        texto(textoPie, " de ", 8, false, "555555");
        campo(textoPie, "NUMPAGES");
    }

    /** Campo de Word (PAGE, NUMPAGES) que se actualiza al abrir el documento. */
    private void campo(XWPFParagraph parrafo, String instruccion) {
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
