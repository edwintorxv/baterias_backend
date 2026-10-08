package com.riesgopsicosocial.domain.model.resultado.configuracion;

/**
 * Respuestas a las preguntas filtro guardadas en la aplicación. {@code null} significa
 * que no se registró: en ese caso la dimensión se trata como si aplicara.
 */
public record FiltrosAplicacion(Boolean atiendeClientes, Boolean esJefe) {

    public static final FiltrosAplicacion SIN_REGISTRO = new FiltrosAplicacion(null, null);

    /** {@code true} solo cuando el evaluado respondió explícitamente "no" a la pregunta filtro. */
    public boolean noAplica(CondicionAplicacion condicion) {
        Boolean valor = switch (condicion) {
            case ATIENDE_CLIENTES -> atiendeClientes;
            case ES_JEFE -> esJefe;
        };
        return Boolean.FALSE.equals(valor);
    }

}
