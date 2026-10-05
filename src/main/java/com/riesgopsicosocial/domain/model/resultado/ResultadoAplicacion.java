package com.riesgopsicosocial.domain.model.resultado;

import java.time.LocalDateTime;
import java.util.List;

public record ResultadoAplicacion(
        Long idAplicacion,
        Long idGrupoOcupacional,
        LocalDateTime fechaCalculo,
        List<ResultadoCuestionario> cuestionarios
) {
}
