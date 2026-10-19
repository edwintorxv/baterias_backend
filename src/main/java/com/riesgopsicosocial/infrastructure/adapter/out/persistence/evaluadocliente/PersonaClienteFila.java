package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import java.time.LocalDateTime;

/**
 * Una persona en un cliente (relación evaluado-cliente) con sus datos y su última aplicación, en una
 * sola consulta para el listado del front. Los alias de la consulta nativa coinciden con estos getters.
 */
public interface PersonaClienteFila {

    Long getIdRelacion();

    Long getIdEvaluado();

    String getNumeroIdentificacion();

    String getNombre();

    String getApellido();

    String getNombreCargo();

    String getTipoCargo();

    String getNombreArea();

    String getCiudadTrabajo();

    Boolean getActivo();

    Long getAplicaciones();

    Long getIdUltimaAplicacion();

    LocalDateTime getFechaUltimaAplicacion();

    String getEstadoUltimaAplicacion();

    Boolean getUltimaConRespuestas();

    Boolean getUltimaConResultados();

}
