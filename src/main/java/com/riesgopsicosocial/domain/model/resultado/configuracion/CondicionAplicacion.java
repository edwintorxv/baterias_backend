package com.riesgopsicosocial.domain.model.resultado.configuracion;

/**
 * Pregunta filtro de la aplicación de la que depende una dimensión. Si el evaluado
 * responde "no", los ítems de la dimensión no se responden y su puntaje bruto es 0.
 */
public enum CondicionAplicacion {

    /** "En mi trabajo debo brindar servicio a clientes o usuarios" → demandas emocionales. */
    ATIENDE_CLIENTES,

    /** "Soy jefe de otras personas en mi trabajo" → relación con los colaboradores (forma A). */
    ES_JEFE

}
