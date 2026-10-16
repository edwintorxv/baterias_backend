package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Análisis de un grupo de personas (toda la empresa o un área).
 *
 * @param coberturaPorCiudad cobertura por ciudad de trabajo, de la de más personas esperadas a la de menos
 * @param totalGeneral      total general (intralaboral + extralaboral) de quienes tienen las dos pruebas
 * @param pendientesCalculo evaluados que respondieron pero aún no tienen resultados: no entran en las tablas
 * @param dominios          solo las formas intralaborales que alguien del grupo presentó
 */
public record AnalisisGrupo(
        Cobertura cobertura,
        List<CoberturaCiudad> coberturaPorCiudad,
        int pendientesCalculo,
        DistribucionNiveles intralaboral,
        DistribucionNiveles extralaboral,
        DistribucionNiveles totalGeneral,
        DistribucionNiveles estres,
        List<DominiosForma> dominios
) {
}
