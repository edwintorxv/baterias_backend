package com.riesgopsicosocial.domain.model.informe;

import java.util.List;

/**
 * Dimensión del cuestionario de estrés (fisiológicos, comportamiento social, intelectuales y laborales,
 * psicoemocionales) con los síntomas que el trabajador reportó con frecuencia. El manual no trae baremos
 * por dimensión: no hay puntaje ni nivel, solo los síntomas.
 *
 * @param sintomas del más frecuente al menos (Siempre antes que Casi siempre); empate: orden del cuestionario.
 *                 Vacía si no reportó ninguno con frecuencia
 */
public record GrupoSintomas(String nombre, List<SintomaFrecuente> sintomas) {
}
