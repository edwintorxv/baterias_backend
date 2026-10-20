package com.riesgopsicosocial.application.port.out.informe;

import com.riesgopsicosocial.domain.model.informe.DatosAplicacion;
import com.riesgopsicosocial.domain.model.informe.DatosCliente;
import com.riesgopsicosocial.domain.model.informe.DatosEvaluado;
import com.riesgopsicosocial.domain.model.informe.FirmaEvaluador;
import com.riesgopsicosocial.domain.model.informe.empresa.DimensionInforme;
import com.riesgopsicosocial.domain.model.informe.empresa.ItemSintoma;
import com.riesgopsicosocial.domain.model.informe.empresa.OpcionFrecuencia;
import com.riesgopsicosocial.domain.model.informe.empresa.RespuestaSintoma;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InformeEvaluadoPort {

    Optional<DatosCliente> buscarCliente(Long idCliente);

    Optional<DatosEvaluado> buscarEvaluado(Long idEvaluado);

    /** Si el evaluado tiene o tuvo alguna relación (activa o no) con el cliente. */
    boolean existeRelacion(Long idCliente, Long idEvaluado);

    /** Aplicaciones del evaluado con el cliente, en todas sus relaciones (sin orden garantizado). */
    List<DatosAplicacion> buscarAplicaciones(Long idCliente, Long idEvaluado);

    /** Vacío si el evaluador no ha cargado su firma. */
    Optional<FirmaEvaluador> buscarFirma(Long idEvaluador);

    /** Dimensiones del cuestionario de estrés, en el orden del cuestionario (fisiológicos primero). */
    List<DimensionInforme> buscarDimensionesEstres();

    /** Ítems (síntomas) del cuestionario de estrés, con su texto. */
    List<ItemSintoma> buscarItemsEstres();

    /** Opciones del cuestionario de estrés, de la más frecuente (Siempre) a la menos (Nunca). */
    List<OpcionFrecuencia> buscarOpcionesEstres();

    /** Respuestas a los ítems de estrés de esas aplicaciones. */
    List<RespuestaSintoma> buscarRespuestasEstres(Collection<Long> idsAplicacion);

}
