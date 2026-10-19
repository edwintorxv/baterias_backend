package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import java.util.Map;

/**
 * Una fila con datos de la hoja "Personas", tal como viene (texto recortado; las fechas como yyyy-MM-dd).
 *
 * @param numero número de la fila en Excel (la 2 es la primera después del encabezado)
 */
public record FilaArchivo(int numero, Map<ColumnaPersona, String> valores) {

    /** {@code null} si la celda está vacía. */
    public String valor(ColumnaPersona columna) {
        return valores.get(columna);
    }

}
