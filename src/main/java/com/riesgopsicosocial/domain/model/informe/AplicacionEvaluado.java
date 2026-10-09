package com.riesgopsicosocial.domain.model.informe;

import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;

/**
 * @param edad      edad del evaluado en el año de la aplicación ({@code null} si falta el año de nacimiento)
 * @param resultado {@code null} si la aplicación aún no tiene resultados calculados
 */
public record AplicacionEvaluado(DatosAplicacion datos, Integer edad, ResultadoAplicacion resultado) {

    public boolean tieneResultados() {
        return resultado != null;
    }

}
