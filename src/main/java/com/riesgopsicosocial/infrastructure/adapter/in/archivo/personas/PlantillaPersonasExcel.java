package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente.VinculacionPersonaService;
import com.riesgopsicosocial.shared.exception.ValidationException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Plantilla Excel para cargar las personas de un cliente: hoja "Personas" (encabezado fijo, listas
 * desplegables, validación de números y fechas), "Instrucciones" y "Listas" (oculta, con los valores).
 * También la lee: una {@link FilaArchivo} por fila con datos.
 */
@Component
public class PlantillaPersonasExcel {

    static final String HOJA_PERSONAS = "Personas";
    static final String HOJA_INSTRUCCIONES = "Instrucciones";
    static final String HOJA_LISTAS = "Listas";
    public static final int MAXIMO_FILAS = 2000;

    private static final ColumnaPersona[] COLUMNAS = ColumnaPersona.values();

    public byte[] generar(CatalogosCarga catalogos) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            XSSFSheet personas = libro.createSheet(HOJA_PERSONAS);
            XSSFSheet instrucciones = libro.createSheet(HOJA_INSTRUCCIONES);
            XSSFSheet listas = libro.createSheet(HOJA_LISTAS);

            escribirListas(libro, listas, catalogos);
            escribirEncabezado(libro, personas);
            validaciones(personas);
            escribirInstrucciones(libro, instrucciones);

            libro.setSheetHidden(libro.getSheetIndex(listas), true);
            libro.setActiveSheet(0);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar la plantilla de personas", e);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Generar
    // ---------------------------------------------------------------------------------------------

    private static String nombreLista(ListaPlantilla lista) {
        return "Lista_" + lista.name();
    }

    /** Una columna por catálogo, con un nombre definido para usarlo en las listas desplegables. */
    private static void escribirListas(XSSFWorkbook libro, XSSFSheet hoja, CatalogosCarga catalogos) {
        Row titulos = hoja.createRow(0);
        ListaPlantilla[] listas = ListaPlantilla.values();
        for (int c = 0; c < listas.length; c++) {
            titulos.createCell(c).setCellValue(listas[c].name());
            List<CatalogosCarga.Opcion> opciones = catalogos.opciones(listas[c]);
            for (int f = 0; f < opciones.size(); f++) {
                Row fila = hoja.getRow(f + 1) != null ? hoja.getRow(f + 1) : hoja.createRow(f + 1);
                fila.createCell(c).setCellValue(opciones.get(f).etiqueta());
            }
            String columna = CellReference.convertNumToColString(c);
            XSSFName nombre = libro.createName();
            nombre.setNameName(nombreLista(listas[c]));
            nombre.setRefersToFormula("'" + HOJA_LISTAS + "'!$" + columna + "$2:$" + columna + "$"
                    + Math.max(2, opciones.size() + 1));
        }
    }

    private static void escribirEncabezado(XSSFWorkbook libro, XSSFSheet hoja) {
        CellStyle obligatoria = estiloEncabezado(libro, new byte[]{(byte) 0xF8, (byte) 0xCB, (byte) 0xAD});
        CellStyle opcional = estiloEncabezado(libro, new byte[]{(byte) 0xD9, (byte) 0xE1, (byte) 0xF2});
        CellStyle texto = libro.createCellStyle();
        texto.setDataFormat(libro.createDataFormat().getFormat("@"));
        CellStyle fecha = libro.createCellStyle();
        fecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy"));

        Row encabezado = hoja.createRow(0);
        encabezado.setHeightInPoints(32);
        for (int c = 0; c < COLUMNAS.length; c++) {
            ColumnaPersona columna = COLUMNAS[c];
            Cell celda = encabezado.createCell(c);
            celda.setCellValue(columna.encabezado());
            celda.setCellStyle(columna.obligatoria() ? obligatoria : opcional);
            hoja.setColumnWidth(c, columna.ancho() * 256);
            // El documento como texto (ceros a la izquierda, sin notación científica); la fecha con formato.
            if (columna == ColumnaPersona.DOCUMENTO) {
                hoja.setDefaultColumnStyle(c, texto);
            } else if (columna.tipo() == ColumnaPersona.Tipo.FECHA) {
                hoja.setDefaultColumnStyle(c, fecha);
            }
        }
        hoja.createFreezePane(0, 1);
    }

    private static CellStyle estiloEncabezado(XSSFWorkbook libro, byte[] rgb) {
        XSSFCellStyle estilo = libro.createCellStyle();
        XSSFFont negrita = libro.createFont();
        negrita.setBold(true);
        estilo.setFont(negrita);
        estilo.setFillForegroundColor(new XSSFColor(rgb, null));
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setWrapText(true);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setBorderBottom(BorderStyle.THIN);
        return estilo;
    }

    private static void validaciones(XSSFSheet hoja) {
        DataValidationHelper ayuda = hoja.getDataValidationHelper();
        int anioActual = LocalDate.now(ZoneId.of("America/Bogota")).getYear();
        for (int c = 0; c < COLUMNAS.length; c++) {
            ColumnaPersona columna = COLUMNAS[c];
            CellRangeAddressList celdas = new CellRangeAddressList(1, MAXIMO_FILAS, c, c);
            DataValidationConstraint regla;
            String mensaje;
            switch (columna.tipo()) {
                case LISTA -> {
                    regla = ayuda.createFormulaListConstraint(nombreLista(columna.lista()));
                    mensaje = "Elija un valor de la lista.";
                }
                case ENTERO -> {
                    if (columna == ColumnaPersona.ANIO_NACIMIENTO) {
                        regla = ayuda.createIntegerConstraint(DataValidationConstraint.OperatorType.BETWEEN,
                                String.valueOf(anioActual - VinculacionPersonaService.EDAD_MAXIMA),
                                String.valueOf(anioActual - VinculacionPersonaService.EDAD_MINIMA));
                        mensaje = "Año de 4 cifras (edad de " + VinculacionPersonaService.EDAD_MINIMA + " a "
                                + VinculacionPersonaService.EDAD_MAXIMA + " años).";
                    } else {
                        regla = ayuda.createIntegerConstraint(DataValidationConstraint.OperatorType.BETWEEN, "0", "30");
                        mensaje = "Número entero de 0 a 30.";
                    }
                }
                case FECHA -> {
                    regla = ayuda.createDateConstraint(DataValidationConstraint.OperatorType.BETWEEN,
                            "=DATE(1950,1,1)", "=DATE(2100,12,31)", "dd/mm/yyyy");
                    mensaje = "Fecha (día/mes/año). Vacía = hoy.";
                }
                default -> {
                    continue;
                }
            }
            DataValidation validacion = ayuda.createValidation(regla, celdas);
            validacion.setShowErrorBox(true);
            validacion.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validacion.createErrorBox(columna.titulo(), mensaje);
            validacion.setSuppressDropDownArrow(columna.tipo() == ColumnaPersona.Tipo.LISTA);
            hoja.addValidationData(validacion);
        }
    }

    private static void escribirInstrucciones(XSSFWorkbook libro, XSSFSheet hoja) {
        CellStyle titulo = libro.createCellStyle();
        Font negrita = libro.createFont();
        negrita.setBold(true);
        negrita.setFontHeightInPoints((short) 13);
        titulo.setFont(negrita);
        CellStyle parrafo = libro.createCellStyle();
        parrafo.setWrapText(true);
        parrafo.setVerticalAlignment(VerticalAlignment.TOP);

        String obligatorias = String.join(", ", Arrays.stream(COLUMNAS)
                .filter(ColumnaPersona::obligatoria).map(ColumnaPersona::titulo).toList());
        List<String> lineas = List.of(
                "Cómo diligenciar la plantilla",
                "1. Una fila por persona en la hoja \"" + HOJA_PERSONAS + "\", desde la fila 2. No cambie ni mueva los encabezados.",
                "2. Obligatorias (marcadas con * y en naranja): " + obligatorias + ".",
                "3. Las columnas con lista desplegable solo aceptan valores de la lista. Si pega datos de otro archivo, "
                        + "se aceptan sin importar mayúsculas, tildes ni espacios.",
                "4. Ciudades: aparecen como CIUDAD – DEPARTAMENTO. También puede escribir solo la ciudad si su nombre "
                        + "no se repite en otro departamento.",
                "5. El documento no puede repetirse dentro del archivo. Máximo " + MAXIMO_FILAS + " personas por archivo.",
                "6. Fecha de vinculación: vacía = el día de la carga.",
                "7. Si la persona ya existe (mismo documento), se actualizan sus datos con los del archivo. Si ya está "
                        + "vinculada a este cliente, se actualiza su vinculación; si está activa en otra empresa, allí "
                        + "se inactiva y queda activa en este cliente (su evaluación anterior se conserva).",
                "8. Primero se valida el archivo y se muestra qué pasará con cada fila; nada se guarda hasta confirmar. "
                        + "Si alguna fila tiene errores, no se guarda ninguna.");
        hoja.setColumnWidth(0, 110 * 256);
        for (int i = 0; i < lineas.size(); i++) {
            Cell celda = hoja.createRow(i).createCell(0);
            celda.setCellValue(lineas.get(i));
            celda.setCellStyle(i == 0 ? titulo : parrafo);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Leer
    // ---------------------------------------------------------------------------------------------

    /**
     * Filas con datos de la hoja "Personas" (las vacías se saltan).
     *
     * @throws ValidationException si no es un .xlsx, no tiene las columnas de la plantilla o pasa del máximo de filas
     */
    public List<FilaArchivo> leer(byte[] archivo) {
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            Sheet hoja = libro.getSheet(HOJA_PERSONAS);
            if (hoja == null) {
                throw new ValidationException("El archivo no tiene la hoja \"" + HOJA_PERSONAS + "\"",
                        List.of("Descargue la plantilla y diligencie los datos en ella"));
            }
            validarEncabezado(hoja.getRow(0));
            FormulaEvaluator evaluador = libro.getCreationHelper().createFormulaEvaluator();

            List<FilaArchivo> filas = new ArrayList<>();
            for (int f = 1; f <= hoja.getLastRowNum(); f++) {
                Map<ColumnaPersona, String> valores = valores(hoja.getRow(f), evaluador);
                if (valores.isEmpty()) {
                    continue;
                }
                if (filas.size() == MAXIMO_FILAS) {
                    throw new ValidationException("El archivo tiene más de " + MAXIMO_FILAS + " personas",
                            List.of("Divídalo en varios archivos"));
                }
                filas.add(new FilaArchivo(f + 1, valores));
            }
            return filas;
        } catch (IOException | RuntimeException e) {
            if (e instanceof ValidationException validacion) {
                throw validacion;
            }
            throw new ValidationException("El archivo no es un Excel (.xlsx) válido",
                    List.of("Descargue la plantilla y guárdela como libro de Excel (.xlsx)"));
        }
    }

    private static void validarEncabezado(Row encabezado) {
        List<String> faltan = new ArrayList<>();
        for (int c = 0; c < COLUMNAS.length; c++) {
            String esperado = TextoNormalizado.de(COLUMNAS[c].titulo());
            Cell celda = encabezado == null ? null : encabezado.getCell(c);
            String actual = celda == null ? "" : TextoNormalizado.de(celda.toString().replace("*", ""));
            if (!esperado.equals(actual)) {
                faltan.add("Columna " + CellReference.convertNumToColString(c) + ": se esperaba \"" + COLUMNAS[c].titulo() + "\"");
            }
        }
        if (!faltan.isEmpty()) {
            throw new ValidationException("El archivo no tiene las columnas de la plantilla (descárguela de nuevo)", faltan);
        }
    }

    private static Map<ColumnaPersona, String> valores(Row fila, FormulaEvaluator evaluador) {
        Map<ColumnaPersona, String> valores = new EnumMap<>(ColumnaPersona.class);
        if (fila == null) {
            return valores;
        }
        for (int c = 0; c < COLUMNAS.length; c++) {
            String valor = texto(fila.getCell(c), evaluador);
            if (valor != null) {
                valores.put(COLUMNAS[c], valor);
            }
        }
        return valores;
    }

    /** Texto recortado; números sin ".0" ni notación científica; fechas como yyyy-MM-dd; vacío = {@code null}. */
    private static String texto(Cell celda, FormulaEvaluator evaluador) {
        if (celda == null) {
            return null;
        }
        CellType tipo = celda.getCellType() == CellType.FORMULA ? evaluador.evaluateFormulaCell(celda) : celda.getCellType();
        String valor = switch (tipo) {
            case STRING -> celda.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(celda)
                    ? celda.getLocalDateTimeCellValue().toLocalDate().toString()
                    : BigDecimal.valueOf(celda.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(celda.getBooleanCellValue());
            default -> null;
        };
        if (valor == null) {
            return null;
        }
        valor = valor.replace(' ', ' ').strip();
        return valor.isEmpty() ? null : valor;
    }

}
