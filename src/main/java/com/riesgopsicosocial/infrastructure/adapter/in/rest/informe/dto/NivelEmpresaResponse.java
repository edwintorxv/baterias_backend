package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * Nivel de riesgo intralaboral de la empresa por forma (Resolución 2764 de 2022, art. 3).
 *
 * @param periodicidad            {@code ANUAL} o {@code CADA_DOS_ANIOS}; {@code null} sin resultados intralaborales
 * @param periodicidadDescripcion "Anual" o "Cada dos años"
 */
public record NivelEmpresaResponse(
        List<NivelFormaResponse> formas,
        String periodicidad,
        String periodicidadDescripcion
) {
}
