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

    public InformeVista toVista(InformeEvaluado informe, Map<Long, FirmaEvaluador> firmas, LocalDate fechaElaboracion) {
        DatosEvaluado evaluado = informe.evaluado();
        return new InformeVista(
                informe.cliente().nombre(),
                informe.cliente().nit(),
                evaluado.nombre() + " " + evaluado.apellido(),
                evaluado.numeroIdentificacion(),
                texto(evaluado.sexo()),
                fechaElaboracion.format(FECHA),
                informe.aplicaciones().stream().map(a -> toVista(a, firmas)).toList());
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
                firma == null ? null
                        : "data:" + firma.tipoContenido() + ";base64," + Base64.getEncoder().encodeToString(firma.imagen()),
                firma == null ? null : firma.imagen());
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
