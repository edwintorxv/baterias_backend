package com.riesgopsicosocial.domain.model.resultado;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param totalGeneral intralaboral + extralaboral; {@code null} si la aplicación no tiene ambos
 */
public record ResultadoAplicacion(
        Long idAplicacion,
        Long idGrupoOcupacional,
        LocalDateTime fechaCalculo,
        List<ResultadoCuestionario> cuestionarios,
        ResultadoTotalGeneral totalGeneral
) {
}
