package com.riesgopsicosocial.domain.model.informe;

import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;

/**
 * @param resultado {@code null} si la aplicación aún no tiene resultados calculados
 */
public record AplicacionEvaluado(DatosAplicacion datos, ResultadoAplicacion resultado) {

    public boolean tieneResultados() {
        return resultado != null;
    }

}
