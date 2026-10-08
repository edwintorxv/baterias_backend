package com.riesgopsicosocial.application.port.out.informe;

import com.riesgopsicosocial.domain.model.informe.DatosAplicacion;
import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.DatosEvaluado;

import java.util.List;
import java.util.Optional;

public interface InformeEvaluadoPort {

    Optional<DatosCliente> buscarCliente(Long idCliente);

    Optional<DatosEvaluado> buscarEvaluado(Long idEvaluado);

    /** Si el evaluado tiene o tuvo alguna relación (activa o no) con el cliente. */
    boolean existeRelacion(Long idCliente, Long idEvaluado);

    /** Aplicaciones del evaluado con el cliente, en todas sus relaciones (sin orden garantizado). */
    List<DatosAplicacion> buscarAplicaciones(Long idCliente, Long idEvaluado);

}
