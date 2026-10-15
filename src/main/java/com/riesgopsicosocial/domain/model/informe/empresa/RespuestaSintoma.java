package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Respuesta a un ítem del cuestionario de estrés: en qué aplicación, a qué pregunta (y su dimensión)
 * y con qué frecuencia.
 */
public record RespuestaSintoma(Long idAplicacion, Long idPregunta, Long idDimensionCuestionario, Long idOpcion) {
}
