package com.riesgopsicosocial.application.port.out.informe;

import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.empresa.AplicacionEmpresa;
import com.riesgopsicosocial.domain.model.informe.empresa.CuestionarioInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.PersonaCargada;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;

import java.util.List;
import java.util.Optional;

public interface InformeEmpresaPort {

    Optional<DatosCliente> buscarCliente(Long idCliente);

    /** Relaciones activas del cliente: las personas que se esperan evaluar. */
    List<PersonaCargada> buscarPersonasCargadas(Long idCliente);

    /**
     * Aplicaciones del año en todas las relaciones del cliente, cargadas en bloque.
     * Sus resultados traen cuestionarios y dominios, sin dimensiones.
     */
    List<AplicacionEmpresa> buscarAplicaciones(Long idCliente, int anio);

    /** Años con aplicaciones del cliente (sin orden garantizado). */
    List<Integer> buscarAnios(Long idCliente);

    /** Todos los cuestionarios con sus dominios, en orden. */
    List<CuestionarioInforme> buscarCuestionarios();

    /** Niveles de riesgo en orden (1 a 5). */
    List<NivelRiesgo> buscarNiveles();

}
