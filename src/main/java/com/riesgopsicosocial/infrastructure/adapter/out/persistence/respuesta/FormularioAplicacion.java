package com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta;

import java.util.List;

/**
 * Lo que necesita la pantalla para digitar una aplicación: los cuestionarios que le corresponden (A o B
 * según su grupo ocupacional, más C y D), cada pregunta con sus opciones y la respuesta ya guardada.
 *
 * @param atiendeClientes pregunta filtro; {@code null} = no registrada
 * @param esJefe          pregunta filtro; {@code null} = no registrada
 */
public record FormularioAplicacion(
        Long idAplicacion,
        Long idGrupoOcupacional,
        Boolean atiendeClientes,
        Boolean esJefe,
        EstadoAplicacion estado,
        List<Cuestionario> cuestionarios
) {

    /**
     * @param aplicables  preguntas que hay que responder (sin las que no aplican por una pregunta filtro)
     * @param respondidas de esas, cuántas tienen respuesta
     */
    public record Cuestionario(
            Long id,
            String forma,
            String nombre,
            int aplicables,
            int respondidas,
            List<Pregunta> preguntas
    ) {
    }

    /**
     * @param condicion pregunta filtro de la que depende ({@code ATIENDE_CLIENTES}, {@code ES_JEFE}); {@code null}
     *                  si siempre aplica
     * @param aplica    {@code false} si la pregunta filtro de la aplicación es "no" (no se responde)
     * @param opciones  en el orden de la escala (Siempre … Nunca)
     * @param respuesta id de la opción guardada; {@code null} sin responder
     */
    public record Pregunta(
            Long id,
            int numero,
            String descripcion,
            String condicion,
            boolean aplica,
            List<Opcion> opciones,
            Long respuesta
    ) {
    }

    public record Opcion(Long id, String nombre) {
    }

}
