package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import com.riesgopsicosocial.application.port.out.informe.GeneradorDocumentoInformePort;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.FormatoDocumento;
import com.riesgopsicosocial.domain.model.informe.InformeEvaluado;
import com.riesgopsicosocial.infrastructure.adapter.out.documento.informe.InformeVista.*;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.riesgopsicosocial.infrastructure.adapter.out.documento.informe.DocxSoporte.*;

/**
 * Informe individual en Word, con el mismo contenido que el PDF ({@link InformeVista}).
 * Queda editable para que el psicólogo pueda ajustar observaciones y recomendaciones.
 */
@Component
public class DocxInformeEvaluadoGenerator implements GeneradorDocumentoInformePort {

    private static final String GRIS_TOTAL = "EEF1F4";
    /** Tonos suaves por nivel, propios del informe individual (el de empresa usa el semáforo de {@link DocxSoporte}). */
    private static final Map<Long, String> COLOR_NIVEL_SUAVE = Map.of(
            1L, "D9F0D3", 2L, "EEF6C9", 3L, "FFF2B3", 4L, "FFD2A8", 5L, "F7B4B0");
    private static final int[] ANCHOS_DATOS = {3876, 6324};
    private static final int[] ANCHOS_INTRALABORAL = {2244, 4182, 1326, 2448};
    private static final int[] ANCHOS_TRES_COLUMNAS = {6426, 1326, 2448};

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
            configurarPagina(documento, "CONFIDENCIAL – Hace parte de la historia clínica ocupacional del trabajador");
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
                celda(fila.getCell(3), dimension.nivel(), false, ParagraphAlignment.CENTER, COLOR_NIVEL_SUAVE.get(dimension.idNivel()), anchos[3]);
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
            celda(fila.getCell(2), dimension.nivel(), false, ParagraphAlignment.CENTER, COLOR_NIVEL_SUAVE.get(dimension.idNivel()), anchos[2]);
        }
        XWPFTableRow fila = tabla.createRow();
        String nombre = totalEnMayusculas ? total.nombre().toUpperCase() : total.nombre();
        celda(fila.getCell(0), nombre, true, ParagraphAlignment.LEFT, GRIS_TOTAL_GENERAL, anchos[0]);
        celda(fila.getCell(1), total.puntaje(), true, ParagraphAlignment.CENTER, GRIS_TOTAL_GENERAL, anchos[1]);
        celda(fila.getCell(2), total.nivel(), true, ParagraphAlignment.CENTER, COLOR_NIVEL_SUAVE.get(total.idNivel()), anchos[2]);
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
        celda(fila.getCell(2), total.nivel(), true, ParagraphAlignment.CENTER, COLOR_NIVEL_SUAVE.get(total.idNivel()),
                ANCHOS_INTRALABORAL[3]);
    }




    private void combinarVertical(XWPFTableCell celda, boolean inicio) {
        propiedades(celda).addNewVMerge().setVal(inicio ? STMerge.RESTART : STMerge.CONTINUE);
    }


    // ---------------------------------------------------------------- textos



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
            imagenFirma(doc, evaluador.firma(), evaluador.firmaTipoContenido());
        }
        XWPFParagraph linea = parrafo(doc, ParagraphAlignment.LEFT, evaluador.firma() == null ? 600 : 0, 0);
        linea.setBorderTop(Borders.SINGLE);
        linea.setIndentationRight(5500);
        texto(linea, "Firma del evaluador", 9.5, false, null);
        texto(parrafo(doc, ParagraphAlignment.LEFT, 0, 0), evaluador.nombre(), 9.5, false, null);
        texto(parrafo(doc, ParagraphAlignment.LEFT, 0, 0),
                "T.P. " + evaluador.tarjetaProfesional() + " – Licencia SO " + evaluador.licenciaSaludOcupacional(), 9.5, false, null);
    }





    // ---------------------------------------------------------------- página



}
