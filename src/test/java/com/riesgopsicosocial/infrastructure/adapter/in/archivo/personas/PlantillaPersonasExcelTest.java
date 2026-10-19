package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import com.riesgopsicosocial.shared.exception.ValidationException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlantillaPersonasExcelTest {

    private final PlantillaPersonasExcel excel = new PlantillaPersonasExcel();

    @Test
    void generar_hojasEncabezadoListasYValidaciones() throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(excel.generar(ArchivoDePrueba.catalogos())))) {
            assertEquals(PlantillaPersonasExcel.HOJA_PERSONAS, libro.getSheetName(0));
            assertEquals(PlantillaPersonasExcel.HOJA_INSTRUCCIONES, libro.getSheetName(1));
            assertTrue(libro.isSheetHidden(libro.getSheetIndex(PlantillaPersonasExcel.HOJA_LISTAS)));

            XSSFSheet personas = libro.getSheet(PlantillaPersonasExcel.HOJA_PERSONAS);
            Row encabezado = personas.getRow(0);
            assertEquals("Documento *", encabezado.getCell(0).getStringCellValue());
            assertEquals("Estado civil", encabezado.getCell(ColumnaPersona.ESTADO_CIVIL.ordinal()).getStringCellValue());
            assertEquals(ColumnaPersona.values().length, encabezado.getLastCellNum());
            assertEquals(1, personas.getLastRowNum() + 1, "sin datos: solo el encabezado");

            // 12 listas + año + personas a cargo + fecha
            assertEquals(15, personas.getDataValidations().size());
            assertEquals("'Listas'!$D$2:$D$5", libro.getName("Lista_CIUDAD").getRefersToFormula());
        }
    }

    @Test
    void leer_saltaFilasVaciasYConvierteNumerosYFechas() {
        Map<ColumnaPersona, Object> primera = ArchivoDePrueba.persona("x");
        primera.put(ColumnaPersona.DOCUMENTO, 1010123456d); // Excel lo guardó como número
        primera.put(ColumnaPersona.FECHA_VINCULACION, LocalDate.of(2026, 3, 1));
        primera.put(ColumnaPersona.AREA, "  Ventas  ");
        Map<ColumnaPersona, Object> segunda = ArchivoDePrueba.persona("2020");

        List<FilaArchivo> filas = excel.leer(ArchivoDePrueba.archivo(primera, null, segunda));

        assertEquals(2, filas.size());
        assertEquals(2, filas.get(0).numero());
        assertEquals(4, filas.get(1).numero());
        assertEquals("1010123456", filas.get(0).valor(ColumnaPersona.DOCUMENTO));
        assertEquals("2026-03-01", filas.get(0).valor(ColumnaPersona.FECHA_VINCULACION));
        assertEquals("Ventas", filas.get(0).valor(ColumnaPersona.AREA));
        assertEquals(String.valueOf(LocalDate.now().getYear() - 30), filas.get(0).valor(ColumnaPersona.ANIO_NACIMIENTO));
        assertNull(filas.get(0).valor(ColumnaPersona.CARGO));
    }

    @Test
    void leer_encabezadoCambiado_400ConLaColumna() throws IOException {
        byte[] archivo;
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(excel.generar(ArchivoDePrueba.catalogos())));
             var salida = new java.io.ByteArrayOutputStream()) {
            libro.getSheet(PlantillaPersonasExcel.HOJA_PERSONAS).getRow(0).getCell(1).setCellValue("Nombre completo");
            libro.write(salida);
            archivo = salida.toByteArray();
        }

        ValidationException error = assertThrows(ValidationException.class, () -> excel.leer(archivo));
        assertEquals(List.of("Columna B: se esperaba \"Nombres\""), error.getDetails());
    }

    @Test
    void leer_noEsExcel_400() {
        assertThrows(ValidationException.class, () -> excel.leer("documento;nombre".getBytes()));
    }

}
