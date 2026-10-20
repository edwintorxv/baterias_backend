package com.riesgopsicosocial.domain.model.informe;

import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;

import java.util.List;

/**
 * @param edad               edad del evaluado en el año de la aplicación ({@code null} si falta el año de nacimiento)
 * @param resultado          {@code null} si la aplicación aún no tiene resultados calculados
 * @param sintomasFrecuentes síntomas de estrés reportados con frecuencia, por dimensión; vacía si la
 *                           aplicación no tiene respuestas de estrés
 */
public record AplicacionEvaluado(DatosAplicacion datos, Integer edad, ResultadoAplicacion resultado,
                                 List<GrupoSintomas> sintomasFrecuentes) {

    public boolean tieneResultados() {
        return resultado != null;
    }

}
