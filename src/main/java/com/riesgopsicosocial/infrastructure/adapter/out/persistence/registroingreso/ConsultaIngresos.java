package com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso;

import java.time.LocalDate;
import java.util.List;

/**
 * Intentos de ingreso de un rango de días (del más reciente al más antiguo). Los totales cuentan todo el rango;
 * {@code truncado} dice que las filas se cortaron en {@link RegistroIngresoService#MAXIMO_FILAS}.
 */
public record ConsultaIngresos(
        LocalDate desde,
        LocalDate hasta,
        long exitosos,
        long fallidos,
        long bloqueados,
        boolean truncado,
        List<IngresoFila> filas
) {
}
