package com.riesgopsicosocial.infrastructure.adapter.out.documento.informe;

import com.riesgopsicosocial.domain.model.informe.*;
import com.riesgopsicosocial.domain.model.resultado.*;
import com.riesgopsicosocial.infrastructure.adapter.out.documento.informe.InformeVista.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Prepara el informe según los formatos modelo de la batería: intralaboral (Anexos 4 y 5),
 * extralaboral (Anexo 2) y estrés (sus niveles se muestran con las etiquetas de la Tabla 6
 * del manual de estrés, no con las de riesgo).
 */
@Component
public class InformeVistaMapper {

    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SIN_DATO = "—";
    private static final Map<Long, String> NIVELES_ESTRES = Map.of(
            1L, "Muy bajo", 2L, "Bajo", 3L, "Medio", 4L, "Alto", 5L, "Muy alto");

    /** Interpretación genérica de los niveles de riesgo (formatos modelo, Anexos 4 y 5 intralaboral). */
    private static final List<ParrafoVista> INTERPRETACION_RIESGO = List.of(
            new ParrafoVista("Sin riesgo o riesgo despreciable", "ausencia de riesgo o riesgo tan bajo que no amerita desarrollar actividades de intervención. Las dimensiones y dominios que se encuentren bajo esta categoría serán objeto de acciones o programas de promoción."),
            new ParrafoVista("Riesgo bajo", "no se espera que los factores psicosociales que obtengan puntuaciones de este nivel estén relacionados con síntomas o respuestas de estrés significativas. Las dimensiones y dominios que se encuentren bajo esta categoría serán objeto de acciones o programas de intervención, a fin de mantenerlos en los niveles de riesgo más bajos posibles."),
            new ParrafoVista("Riesgo medio", "nivel de riesgo en el que se esperaría una respuesta de estrés moderada. Las dimensiones y dominios que se encuentren bajo esta categoría ameritan observación y acciones sistemáticas de intervención para prevenir efectos perjudiciales en la salud."),
            new ParrafoVista("Riesgo alto", "nivel de riesgo que tiene una importante posibilidad de asociación con respuestas de estrés alto y por tanto, las dimensiones y dominios que se encuentren bajo esta categoría requieren intervención en el marco de un sistema de vigilancia epidemiológica."),
            new ParrafoVista("Riesgo muy alto", "nivel de riesgo con amplia posibilidad de asociarse a respuestas muy altas de estrés. Por consiguiente las dimensiones y dominios que se encuentren bajo esta categoría requieren intervención inmediata en el marco de un sistema de vigilancia epidemiológica."));

    /** Interpretación de los niveles de estrés (manual del cuestionario de estrés, paso 5). */
    private static final List<ParrafoVista> INTERPRETACION_ESTRES = List.of(
            new ParrafoVista("Muy bajo", "ausencia de síntomas de estrés u ocurrencia muy rara que no amerita desarrollar actividades de intervención específicas, salvo acciones o programas de promoción en salud."),
            new ParrafoVista("Bajo", "es indicativo de baja frecuencia de síntomas de estrés y por tanto escasa afectación del estado general de salud. Es pertinente desarrollar acciones o programas de intervención, a fin de mantener la baja frecuencia de síntomas."),
            new ParrafoVista("Medio", "la presentación de síntomas es indicativa de una respuesta de estrés moderada. Los síntomas más frecuentes y críticos ameritan observación y acciones sistemáticas de intervención para prevenir efectos perjudiciales en la salud. Además, se sugiere identificar los factores de riesgo psicosocial intra y extralaboral que pudieran tener alguna relación con los efectos identificados."),
            new ParrafoVista("Alto", "la cantidad de síntomas y su frecuencia de presentación es indicativa de una respuesta de estrés alto. Los síntomas más críticos y frecuentes requieren intervención en el marco de un sistema de vigilancia epidemiológica. Además es muy importante identificar los factores de riesgo psicosocial intra y extralaboral que pudieran tener alguna relación con los efectos identificados."),
            new ParrafoVista("Muy alto", "la cantidad de síntomas y su frecuencia de presentación es indicativa de una respuesta de estrés severa y perjudicial para la salud. Los síntomas más críticos y frecuentes requieren intervención inmediata en el marco de un sistema de vigilancia epidemiológica. Así mismo, es imperativo identificar los factores de riesgo psicosocial intra y extralaboral que pudieran tener alguna relación con los efectos identificados."));

    public InformeVista toVista(InformeEvaluado informe, Map<Long, FirmaEvaluador> firmas, LocalDate fechaElaboracion) {
        DatosEvaluado evaluado = informe.evaluado();
        return new InformeVista(
                informe.cliente().nombre(),
                informe.cliente().nit(),
                evaluado.nombre() + " " + evaluado.apellido(),
                evaluado.numeroIdentificacion(),
                texto(evaluado.sexo()),
                fechaElaboracion.format(FECHA),
                informe.aplicaciones().stream().map(a -> toVista(a, firmas)).toList(),
                INTERPRETACION_RIESGO,
                INTERPRETACION_ESTRES);
    }

    private AplicacionVista toVista(AplicacionEvaluado aplicacion, Map<Long, FirmaEvaluador> firmas) {
        DatosAplicacion datos = aplicacion.datos();
        ResultadoAplicacion resultado = aplicacion.resultado();
        List<CuestionarioVista> cuestionarios = resultado.cuestionarios().stream().map(this::toVista).toList();
        return new AplicacionVista(
                datos.fechaAplicacion().format(FECHA),
                texto(datos.nombreCargo()),
                texto(datos.nombreArea()),
                aplicacion.edad() == null ? SIN_DATO : aplicacion.edad() + " años",
                texto(datos.grupoOcupacional()),
                toVista(datos.evaluador(), firmas.get(datos.evaluador().id())),
                cuestionarios,
                toVista(resultado.totalGeneral()),
                cuestionarios.stream().anyMatch(c -> c.tipo() == TipoCuestionario.ESTRES),
                datos.observaciones(),
                datos.recomendaciones());
    }

    private EvaluadorVista toVista(DatosEvaluador evaluador, FirmaEvaluador firma) {
        return new EvaluadorVista(
                evaluador.nombre(),
                evaluador.numeroIdentificacion(),
                evaluador.profesion(),
                texto(evaluador.posgrado()),
                evaluador.tarjetaProfesional(),
                evaluador.licenciaSaludOcupacional(),
                evaluador.fechaExpedicionLicencia().format(FECHA),
                firma == null ? null : firma.imagen(),
                firma == null ? null : firma.tipoContenido());
    }

    private CuestionarioVista toVista(ResultadoCuestionario rc) {
        return switch (rc.forma()) {
            case "A", "B" -> intralaboral(rc);
            case "C" -> new CuestionarioVista(
                    "Cuestionario de factores de riesgo psicosocial extralaboral",
                    TipoCuestionario.EXTRALABORAL,
                    List.of(),
                    rc.dimensiones().stream().map(d -> fila(d.nombre(), d.puntajeTransformado(), d.nivelRiesgo())).toList(),
                    fila("Total general factores de riesgo psicosocial extralaboral", rc.puntajeTransformado(), rc.nivelRiesgo()),
                    "Nivel de riesgo");
            case "D" -> new CuestionarioVista(
                    "Cuestionario para la evaluación del estrés – Tercera versión",
                    TipoCuestionario.ESTRES,
                    List.of(),
                    List.of(),
                    new FilaVista("Puntaje total de síntomas de estrés", puntaje(rc.puntajeTransformado()),
                            NIVELES_ESTRES.get(rc.nivelRiesgo().id()), rc.nivelRiesgo().id()),
                    "Nivel de síntomas de estrés");
            default -> throw new IllegalStateException("Forma de cuestionario no soportada en el informe: " + rc.forma());
        };
    }

    /** Dimensiones agrupadas bajo su dominio, en el orden de la configuración, como en el manual. */
    private CuestionarioVista intralaboral(ResultadoCuestionario rc) {
        List<DominioVista> dominios = rc.dominios().stream()
                .map(dominio -> new DominioVista(
                        dominio.nombre(),
                        rc.dimensiones().stream()
                                .filter(d -> dominio.idDominioCuestionario().equals(d.idDominioCuestionario()))
                                .map(d -> fila(d.nombre(), d.puntajeTransformado(), d.nivelRiesgo()))
                                .toList(),
                        fila(dominio.nombre().toUpperCase(ES_CO), dominio.puntajeTransformado(), dominio.nivelRiesgo())))
                .toList();
        return new CuestionarioVista(
                "Cuestionario de factores de riesgo psicosocial intralaboral – Forma " + rc.forma(),
                TipoCuestionario.INTRALABORAL,
                dominios,
                List.of(),
                fila("Total general factores de riesgo psicosocial intralaboral", rc.puntajeTransformado(), rc.nivelRiesgo()),
                "Nivel de riesgo");
    }

    private FilaVista toVista(ResultadoTotalGeneral total) {
        if (total == null) {
            return null;
        }
        return fila("Puntaje total general de factores de riesgo psicosocial (intralaboral forma "
                + total.formaIntralaboral() + " + extralaboral)", total.puntajeTransformado(), total.nivelRiesgo());
    }

    private static FilaVista fila(String nombre, BigDecimal puntaje, NivelRiesgo nivel) {
        return new FilaVista(nombre, puntaje(puntaje), nivel.nombre(), nivel.id());
    }

    /** Un decimal con coma, como los baremos del manual. */
    private static String puntaje(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(ES_CO));
        return formato.format(valor);
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? SIN_DATO : valor;
    }

}
