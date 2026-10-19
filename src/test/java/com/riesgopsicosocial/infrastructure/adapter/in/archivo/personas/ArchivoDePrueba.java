package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas.CatalogosCarga.Opcion;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/** Catálogos pequeños y archivos de la plantilla diligenciados, para las pruebas. */
final class ArchivoDePrueba {

    private ArchivoDePrueba() {
    }

    static CatalogosCarga catalogos() {
        Map<ListaPlantilla, List<Opcion>> opciones = new EnumMap<>(ListaPlantilla.class);
        for (ListaPlantilla lista : ListaPlantilla.values()) {
            opciones.put(lista, List.of(new Opcion(1L, "Opción uno"), new Opcion(2L, "Opción dos")));
        }
        opciones.put(ListaPlantilla.SEXO, List.of(new Opcion(1L, "Femenino"), new Opcion(2L, "Masculino")));
        opciones.put(ListaPlantilla.NIVEL_ACADEMICO, List.of(new Opcion(5L, "Bachillerato completo"),
                new Opcion(7L, "Técnico / tecnológico completo")));
        opciones.put(ListaPlantilla.TIPO_CARGO, List.of(new Opcion(4L, "Operario, operador, ayudante, servicios generales")));
        opciones.put(ListaPlantilla.CIUDAD, List.of(new Opcion(1L, "MEDELLIN – ANTIOQUIA"),
                new Opcion(149L, "BOGOTA, D.C. – BOGOTA"), new Opcion(20L, "SAN LUIS – ANTIOQUIA"),
                new Opcion(21L, "SAN LUIS – TOLIMA")));
        return new CatalogosCarga(opciones, Map.of("MEDELLIN", List.of(1L), "BOGOTA, D.C.", List.of(149L),
                "SAN LUIS", List.of(20L, 21L)));
    }

    /** Valores mínimos válidos de una persona. */
    static Map<ColumnaPersona, Object> persona(String documento) {
        Map<ColumnaPersona, Object> valores = new EnumMap<>(ColumnaPersona.class);
        valores.put(ColumnaPersona.DOCUMENTO, documento);
        valores.put(ColumnaPersona.NOMBRE, "Ana");
        valores.put(ColumnaPersona.APELLIDO, "Pérez");
        valores.put(ColumnaPersona.SEXO, "Femenino");
        valores.put(ColumnaPersona.ANIO_NACIMIENTO, (double) (LocalDate.now().getYear() - 30));
        valores.put(ColumnaPersona.NIVEL_ACADEMICO, "Bachillerato completo");
        valores.put(ColumnaPersona.TIPO_CARGO, "Operario, operador, ayudante, servicios generales");
        return valores;
    }

    /** La plantilla generada, con esas filas desde la 2 ({@code null} = fila vacía). */
    @SafeVarargs
    static byte[] archivo(Map<ColumnaPersona, Object>... filas) {
        byte[] plantilla = new PlantillaPersonasExcel().generar(catalogos());
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(plantilla));
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.getSheet(PlantillaPersonasExcel.HOJA_PERSONAS);
            for (int f = 0; f < filas.length; f++) {
                if (filas[f] == null) {
                    continue;
                }
                Row fila = hoja.createRow(f + 1);
                filas[f].forEach((columna, valor) -> {
                    var celda = fila.createCell(columna.ordinal());
                    if (valor instanceof Double numero) {
                        celda.setCellValue(numero);
                    } else if (valor instanceof LocalDate fecha) {
                        celda.setCellValue(fecha);
                        celda.setCellStyle(hoja.getColumnStyle(columna.ordinal()));
                    } else {
                        celda.setCellValue(String.valueOf(valor));
                    }
                });
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
