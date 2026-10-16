package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

/**
 * @param nivelEmpresa nivel de riesgo intralaboral de la empresa por forma y periodicidad de la evaluación
 * @param totalGeneral intralaboral + extralaboral, de quienes tienen las dos pruebas
 * @param estres       niveles con las etiquetas del manual de estrés (Muy bajo … Muy alto)
 */
public record ResultadosGlobalesResponse(
        DistribucionNivelesResponse intralaboral,
        NivelEmpresaResponse nivelEmpresa,
        DistribucionNivelesResponse extralaboral,
        DistribucionNivelesResponse totalGeneral,
        DistribucionNivelesResponse estres
) {
}
