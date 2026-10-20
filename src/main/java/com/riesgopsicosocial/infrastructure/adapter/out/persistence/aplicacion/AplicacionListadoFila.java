package com.riesgopsicosocial.infrastructure.adapter.out.persistence.aplicacion;

import java.time.LocalDateTime;

/** Una aplicación para el listado general, en una sola consulta. Los alias de la consulta nativa coinciden con estos getters. */
public interface AplicacionListadoFila {

    Long getId();

    LocalDateTime getFechaAplicacion();

    Long getIdCliente();

    String getCliente();

    Long getIdRelacion();

    Boolean getRelacionActiva();

    Long getIdEvaluado();

    String getNumeroIdentificacion();

    String getNombre();

    String getApellido();

    String getForma();

    Long getIdEvaluador();

    String getEvaluador();

    Long getAplicables();

    Long getRespondidas();

    Boolean getConRespuestas();

    Boolean getConResultados();

}
