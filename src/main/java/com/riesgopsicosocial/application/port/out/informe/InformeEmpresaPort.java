package com.riesgopsicosocial.application.port.out.informe;

import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.empresa.ConclusionesInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.ConfiguracionNivelEmpresa;
import com.riesgopsicosocial.domain.model.informe.empresa.AplicacionEmpresa;
import com.riesgopsicosocial.domain.model.informe.empresa.CuestionarioInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.ItemSintoma;
import com.riesgopsicosocial.domain.model.informe.empresa.OpcionFrecuencia;
import com.riesgopsicosocial.domain.model.informe.empresa.PersonaCargada;
import com.riesgopsicosocial.domain.model.informe.empresa.RespuestaSintoma;
import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;

import java.util.Collection;
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

    /** Factor de transformación y baremo del puntaje total de cada forma intralaboral (A y B). */
    List<ConfiguracionNivelEmpresa> buscarConfiguracionNivelEmpresa();

    /** Niveles de riesgo en orden (1 a 5). */
    List<NivelRiesgo> buscarNiveles();

    /** Respuestas de esas aplicaciones a los ítems del cuestionario de estrés, cargadas en bloque. */
    List<RespuestaSintoma> buscarRespuestasEstres(Collection<Long> idsAplicacion);

    /** Opciones de respuesta del cuestionario de estrés, de la más frecuente (Siempre) a la menos (Nunca). */
    List<OpcionFrecuencia> buscarOpcionesEstres();

    /** Ítems (síntomas) del cuestionario de estrés con su texto. */
    List<ItemSintoma> buscarItemsEstres();

    /** Conclusiones y plan de acción del año con su profesional; vacío si no se ha escrito nada. */
    Optional<ConclusionesInforme> buscarConclusiones(Long idCliente, int anio);

    /** Vacío si el profesional no ha cargado su firma. */
    Optional<FirmaEvaluador> buscarFirma(Long idEvaluador);

}
