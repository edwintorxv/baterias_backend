package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import java.util.List;
import java.util.Map;

/**
 * Qué pasa (o pasó) con cada fila del archivo.
 *
 * @param resumen        cuántas filas por resultado: NUEVA, VINCULA, ACTUALIZA, CAMBIA_EMPRESA y ERROR
 * @param puedeConfirmar ninguna fila tiene errores
 * @param guardado       {@code true} solo en la respuesta de confirmar
 */
public record ResultadoCarga(int totalFilas, Map<String, Integer> resumen, boolean puedeConfirmar, boolean guardado,
                             List<FilaCarga> filas) {

    /**
     * @param fila      número de la fila en Excel
     * @param resultado NUEVA, VINCULA, ACTUALIZA, CAMBIA_EMPRESA o ERROR
     */
    public record FilaCarga(int fila, String documento, String nombre, String resultado, List<String> errores) {
    }

}
