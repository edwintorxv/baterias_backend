package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * Informe de empresa de un año, en el orden del Word. Solo resultados agregados: los grupos (ciudades,
 * áreas) con menos personas que el mínimo vienen juntos en "Otras…" u omitidos.
 */
public record InformeEmpresaResponse(

        ClienteInformeResponse cliente,
        int anio,
        CoberturaResponse cobertura,
        List<CoberturaCiudadResponse> coberturaPorCiudad,
        int pendientesCalculo,
        PoblacionResponse poblacion,
        ResultadosGlobalesResponse resultadosGlobales,
        List<FormaIntralaboralResponse> formas,
        ConcentracionIntralaboralResponse concentracionIntralaboral,
        FactoresExtralaboralesResponse factoresExtralaborales,
        SintomasEstresResponse sintomasEstres

) {
}
